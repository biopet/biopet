/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.sage

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ MultiSampleQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions.Cat
import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsCoverage
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.extensions.tools.SquishBed
import nl.lumc.sasc.biopet.extensions.tools.{ BedtoolsCoverageToCounts, PrefixFastq, SageCountFastq, SageCreateLibrary, SageCreateTagCounts }
import nl.lumc.sasc.biopet.utils.Logging
import org.broadinstitute.gatk.queue.QScript

class Sage(val parent: Configurable) extends QScript with MultiSampleQScript {
  qscript =>
  def this() = this(null)

  var countBed: Option[File] = config("count_bed")
  var squishedCountBed: File = null
  var transcriptome: Option[File] = config("transcriptome")
  var tagsLibrary: Option[File] = config("tags_library")

  override def defaults = Map(
    "bowtie" -> Map(
      "m" -> 1,
      "k" -> 1,
      "best" -> true,
      "strata" -> true,
      "seedmms" -> 1
    ), "mapping" -> Map(
      "aligner" -> "bowtie",
      "skip_flexiprep" -> true,
      "skip_markduplicates" -> true
    ), "flexiprep" -> Map(
      "skip_clip" -> true,
      "skip_trim" -> true
    ), "strandSensitive" -> true
  )

  def summaryFile: File = new File(outputDir, "Sage.summary.json")

  def summaryFiles: Map[String, File] = Map()

  def summarySettings: Map[String, Any] = Map()

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    def summaryFiles: Map[String, File] = Map()

    def summaryStats: Map[String, Any] = Map()

    def makeLibrary(id: String) = new Library(id)
    class Library(libId: String) extends AbstractLibrary(libId) {
      def summaryFiles: Map[String, File] = Map()

      def summaryStats: Map[String, Any] = Map()

      val inputFastq: File = config("R1")
      val prefixFastq: File = createFile(".prefix.fastq")

      val flexiprep = new Flexiprep(qscript)
      flexiprep.sampleId = Some(sampleId)
      flexiprep.libId = Some(libId)

      val mapping = new Mapping(qscript)
      mapping.libId = Some(libId)
      mapping.sampleId = Some(sampleId)

      protected def addJobs(): Unit = {
        inputFiles :+= new InputFile(inputFastq, config("R1_md5"))

        flexiprep.outputDir = new File(libDir, "flexiprep/")
        flexiprep.inputR1 = inputFastq
        add(flexiprep)

        val flexiprepOutput = for ((key, file) <- flexiprep.outputFiles if key.endsWith("output_R1")) yield file
        val pf = new PrefixFastq(qscript)
        pf.inputFastq = flexiprepOutput.head
        pf.outputFastq = prefixFastq
        pf.prefixSeq = config("sage_tag", default = "CATG")
        pf.deps +:= flexiprep.outputFiles("fastq_input_R1")
        qscript.add(pf)

        mapping.inputR1 = pf.outputFastq
        mapping.outputDir = libDir
        add(mapping)

        if (config("library_counts", default = false).asBoolean) {
          addBedtoolsCounts(mapping.finalBamFile, sampleId + "-" + libId, libDir)
          addTablibCounts(pf.outputFastq, sampleId + "-" + libId, libDir)
        }
      }
    }

    protected def addJobs(): Unit = {
      addPerLibJobs()
      val libraryBamfiles = libraries.map(_._2.mapping.finalBamFile).toList
      val libraryFastqFiles = libraries.map(_._2.prefixFastq).toList

      val bamFile: File = if (libraryBamfiles.size == 1) libraryBamfiles.head
      else {
        val mergeSamFiles = MergeSamFiles(qscript, libraryBamfiles, new File(sampleDir, s"$sampleId.bam"))
        qscript.add(mergeSamFiles)
        mergeSamFiles.output
      }
      val fastqFile: File = if (libraryFastqFiles.size == 1) libraryFastqFiles.head
      else {
        val cat = Cat(qscript, libraryFastqFiles, createFile(".fastq"))
        qscript.add(cat)
        cat.output
      }

      addBedtoolsCounts(bamFile, sampleId, sampleDir)
      addTablibCounts(fastqFile, sampleId, sampleDir)
    }
  }

  def init() {
    if (transcriptome.isEmpty && tagsLibrary.isEmpty)
      Logging.addError("No transcriptome or taglib found")
    if (countBed.isEmpty)
      Logging.addError("No bedfile supplied, please add a countBed")
  }

  def biopetScript() {
    val squishBed = new SquishBed(this)
    squishBed.input = countBed.getOrElse(null)
    squishBed.output = new File(outputDir, countBed.getOrElse(new File("fake")).getName.stripSuffix(".bed") + ".squish.bed")
    add(squishBed)
    squishedCountBed = squishBed.output

    if (tagsLibrary.isEmpty) {
      val cdl = new SageCreateLibrary(this)
      cdl.input = transcriptome.getOrElse(null)
      cdl.output = new File(outputDir, "taglib/tag.lib")
      cdl.noAntiTagsOutput = new File(outputDir, "taglib/no_antisense_genes.txt")
      cdl.noTagsOutput = new File(outputDir, "taglib/no_sense_genes.txt")
      cdl.allGenesOutput = new File(outputDir, "taglib/all_genes.txt")
      add(cdl)
      tagsLibrary = Some(cdl.output)
    }

    addSamplesJobs()
  }

  def addMultiSampleJobs(): Unit = {
  }

  def addBedtoolsCounts(bamFile: File, outputPrefix: String, outputDir: File) {
    val bedtoolsSense = BedtoolsCoverage(this, bamFile, squishedCountBed,
      output = Some(new File(outputDir, outputPrefix + ".genome.sense.coverage")),
      depth = false, sameStrand = true, diffStrand = false)
    val countSense = new BedtoolsCoverageToCounts(this)
    countSense.input = bedtoolsSense.output
    countSense.output = new File(outputDir, outputPrefix + ".genome.sense.counts")

    val bedtoolsAntisense = BedtoolsCoverage(this, bamFile, squishedCountBed,
      output = Some(new File(outputDir, outputPrefix + ".genome.antisense.coverage")),
      depth = false, sameStrand = false, diffStrand = true)
    val countAntisense = new BedtoolsCoverageToCounts(this)
    countAntisense.input = bedtoolsAntisense.output
    countAntisense.output = new File(outputDir, outputPrefix + ".genome.antisense.counts")

    val bedtools = BedtoolsCoverage(this, bamFile, squishedCountBed,
      output = Some(new File(outputDir, outputPrefix + ".genome.coverage")),
      depth = false, sameStrand = false, diffStrand = false)
    val count = new BedtoolsCoverageToCounts(this)
    count.input = bedtools.output
    count.output = new File(outputDir, outputPrefix + ".genome.counts")

    add(bedtoolsSense, countSense, bedtoolsAntisense, countAntisense, bedtools, count)
  }

  def addTablibCounts(fastq: File, outputPrefix: String, outputDir: File) {
    val countFastq = new SageCountFastq(this)
    countFastq.input = fastq
    countFastq.output = new File(outputDir, outputPrefix + ".raw.counts")
    add(countFastq)

    val createTagCounts = new SageCreateTagCounts(this)
    createTagCounts.input = countFastq.output
    createTagCounts.tagLib = tagsLibrary.get
    createTagCounts.countSense = new File(outputDir, outputPrefix + ".tagcount.sense.counts")
    createTagCounts.countAllSense = new File(outputDir, outputPrefix + ".tagcount.all.sense.counts")
    createTagCounts.countAntiSense = new File(outputDir, outputPrefix + ".tagcount.antisense.counts")
    createTagCounts.countAllAntiSense = new File(outputDir, outputPrefix + ".tagcount.all.antisense.counts")
    add(createTagCounts)
  }
}

object Sage extends PipelineCommand