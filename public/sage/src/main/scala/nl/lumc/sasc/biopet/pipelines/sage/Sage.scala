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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.sage

import nl.lumc.sasc.biopet.core.{ BiopetQScript, MultiSampleQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Cat
import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsCoverage
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.tools.PrefixFastq
import nl.lumc.sasc.biopet.tools.BedtoolsCoverageToCounts
import nl.lumc.sasc.biopet.scripts.SquishBed
import nl.lumc.sasc.biopet.tools.SageCountFastq
import nl.lumc.sasc.biopet.tools.SageCreateLibrary
import nl.lumc.sasc.biopet.tools.SageCreateTagCounts
import org.broadinstitute.gatk.queue.QScript

class Sage(val root: Configurable) extends QScript with MultiSampleQScript {
  qscript =>
  def this() = this(null)

  @Input(doc = "countBed", required = false)
  var countBed: File = config("count_bed")

  @Input(doc = "squishedCountBed, by suppling this file the auto squish job will be skipped", required = false)
  var squishedCountBed: File = config("squished_count_bed")

  @Input(doc = "Transcriptome, used for generation of tag library", required = false)
  var transcriptome: File = config("transcriptome")

  var tagsLibrary: File = config("tags_library")

  defaults ++= Map("bowtie" -> Map(
    "m" -> 1,
    "k" -> 1,
    "best" -> true,
    "strata" -> true,
    "seedmms" -> 1
  )
  )

  class Sample(sampleId: String) extends AbstractSample(sampleId) {

    class Library(libraryId: String) extends AbstractLibrary(libraryId) {
      val inputFastq: File = config("R1", required = true)
      val prefixFastq: File = new File(getLibraryDir, sampleId + "-" + libraryId + ".prefix.fastq")

      val flexiprep = new Flexiprep(qscript)
      flexiprep.skipClip = true
      flexiprep.skipTrim = true
      flexiprep.sampleName = sampleId
      flexiprep.libraryName = libraryId

      val mapping = new Mapping(qscript)
      mapping.aligner = config("aligner", default = "bowtie")
      mapping.skipFlexiprep = true
      mapping.skipMarkduplicates = true
      mapping.RGLB = libraryId
      mapping.RGSM = sampleId
      mapping.RGPL = config("PL")
      mapping.RGPU = config("PU")
      mapping.RGCN = config("CN")

      def runJobs(): Unit = {
        flexiprep.outputDir = getLibraryDir + "flexiprep/"
        flexiprep.input_R1 = inputFastq
        flexiprep.init
        flexiprep.biopetScript
        addAll(flexiprep.functions)

        val flexiprepOutput = for ((key, file) <- flexiprep.outputFiles if key.endsWith("output_R1")) yield file
        val pf = new PrefixFastq(qscript)
        pf.inputFastq = flexiprepOutput.head
        pf.outputFastq = prefixFastq
        pf.prefixSeq = config("sage_tag", default = "CATG")
        pf.deps +:= flexiprep.outputFiles("fastq_input_R1")
        add(pf)

        mapping.input_R1 = pf.outputFastq
        mapping.outputDir = getLibraryDir
        mapping.init
        mapping.biopetScript
        addAll(mapping.functions)

        if (config("library_counts", default = false).asBoolean) {
          addBedtoolsCounts(mapping.outputFiles("finalBamFile"), sampleId + "-" + libraryId, getLibraryDir)
          addTablibCounts(pf.outputFastq, sampleId + "-" + libraryId, getLibraryDir)
        }
      }
    }

    def runJobs(): Unit = {
      val libraryBamfiles = libraries.map(_._2.mapping.outputFiles("finalBamFile")).toList
      val libraryFastqFiles = libraries.map(_._2.prefixFastq).toList

      val bamFile: File = if (libraryBamfiles.size == 1) libraryBamfiles.head
      else if (libraryBamfiles.size > 1) {
        val mergeSamFiles = MergeSamFiles(qscript, libraryBamfiles, getSampleDir)
        add(mergeSamFiles)
        mergeSamFiles.output
      } else null
      val fastqFile: File = if (libraryFastqFiles.size == 1) libraryFastqFiles.head
      else if (libraryFastqFiles.size > 1) {
        val cat = Cat(qscript, libraryFastqFiles, getSampleDir + sampleId + ".fastq")
        add(cat)
        cat.output
      } else null

      addBedtoolsCounts(bamFile, sampleId, getSampleDir)
      addTablibCounts(fastqFile, sampleId, getSampleDir)
    }
  }

  def init() {
    if (!outputDir.endsWith("/")) outputDir += "/"
    if (transcriptome == null && tagsLibrary == null)
      throw new IllegalStateException("No transcriptome or taglib found")
    if (countBed == null && squishedCountBed == null)
      throw new IllegalStateException("No bedfile supplied, please add a countBed or squishedCountBed")
  }

  def biopetScript() {
    if (squishedCountBed == null) {
      val squishBed = SquishBed(this, countBed, outputDir)
      add(squishBed)
      squishedCountBed = squishBed.output
    }

    if (tagsLibrary == null) {
      val cdl = new SageCreateLibrary(this)
      cdl.input = transcriptome
      cdl.output = outputDir + "taglib/tag.lib"
      cdl.noAntiTagsOutput = outputDir + "taglib/no_antisense_genes.txt"
      cdl.noTagsOutput = outputDir + "taglib/no_sense_genes.txt"
      cdl.allGenesOutput = outputDir + "taglib/all_genes.txt"
      add(cdl)
      tagsLibrary = cdl.output
    }

    runSamplesJobs
  }

  def addBedtoolsCounts(bamFile: File, outputPrefix: String, outputDir: String) {
    val bedtoolsSense = BedtoolsCoverage(this, bamFile, squishedCountBed, outputDir + outputPrefix + ".genome.sense.coverage",
      depth = false, sameStrand = true, diffStrand = false)
    val countSense = new BedtoolsCoverageToCounts(this)
    countSense.input = bedtoolsSense.output
    countSense.output = outputDir + outputPrefix + ".genome.sense.counts"

    val bedtoolsAntisense = BedtoolsCoverage(this, bamFile, squishedCountBed, outputDir + outputPrefix + ".genome.antisense.coverage",
      depth = false, sameStrand = false, diffStrand = true)
    val countAntisense = new BedtoolsCoverageToCounts(this)
    countAntisense.input = bedtoolsAntisense.output
    countAntisense.output = outputDir + outputPrefix + ".genome.antisense.counts"

    val bedtools = BedtoolsCoverage(this, bamFile, squishedCountBed, outputDir + outputPrefix + ".genome.coverage",
      depth = false, sameStrand = false, diffStrand = false)
    val count = new BedtoolsCoverageToCounts(this)
    count.input = bedtools.output
    count.output = outputDir + outputPrefix + ".genome.counts"

    add(bedtoolsSense, countSense, bedtoolsAntisense, countAntisense, bedtools, count)
  }

  def addTablibCounts(fastq: File, outputPrefix: String, outputDir: String) {
    val countFastq = new SageCountFastq(this)
    countFastq.input = fastq
    countFastq.output = outputDir + outputPrefix + ".raw.counts"
    add(countFastq)

    val createTagCounts = new SageCreateTagCounts(this)
    createTagCounts.input = countFastq.output
    createTagCounts.tagLib = tagsLibrary
    createTagCounts.countSense = outputDir + outputPrefix + ".tagcount.sense.counts"
    createTagCounts.countAllSense = outputDir + outputPrefix + ".tagcount.all.sense.counts"
    createTagCounts.countAntiSense = outputDir + outputPrefix + ".tagcount.antisense.counts"
    createTagCounts.countAllAntiSense = outputDir + outputPrefix + ".tagcount.all.antisense.counts"
    add(createTagCounts)
  }
}

object Sage extends PipelineCommand

object SageTest {
  def main(args: Array[String]): Unit = {
    val sage = new Sage()
    println("done")
  }
}