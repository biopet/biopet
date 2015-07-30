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
package nl.lumc.sasc.biopet.pipelines.carp

import java.io.File

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.macs2.Macs2CallPeak
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.bamtobigwig.Bam2Wig
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.QScript

/**
 * Carp pipeline
 * Chip-Seq analysis pipeline
 * This pipeline performs QC,mapping and peak calling
 */
class Carp(val root: Configurable) extends QScript with MultiSampleQScript with SummaryQScript with Reference {
  qscript =>
  def this() = this(null)

  override def defaults = ConfigUtils.mergeMaps(Map(
    "mapping" -> Map(
      "skip_markduplicates" -> true,
      "aligner" -> "bwa-mem"
    )
  ), super.defaults)

  def summaryFile = new File(outputDir, "Carp.summary.json")

  //TODO: Add summary
  def summaryFiles = Map("reference" -> referenceFasta())

  //TODO: Add summary
  def summarySettings = Map("reference" -> referenceSummary)

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    //TODO: Add summary
    def summaryFiles: Map[String, File] = Map()

    //TODO: Add summary
    def summaryStats: Map[String, Any] = Map()

    def makeLibrary(id: String) = new Library(id)
    class Library(libId: String) extends AbstractLibrary(libId) {
      //TODO: Add summary
      def summaryFiles: Map[String, File] = Map()

      //TODO: Add summary
      def summaryStats: Map[String, Any] = Map()

      val mapping = new Mapping(qscript)
      mapping.libId = Some(libId)
      mapping.sampleId = Some(sampleId)
      mapping.outputDir = libDir

      def addJobs(): Unit = {
        if (config.contains("R1")) {
          mapping.input_R1 = config("R1")
          if (config.contains("R2")) mapping.input_R2 = config("R2")
          mapping.init()
          mapping.biopetScript()
          addAll(mapping.functions)

        } else logger.error("Sample: " + sampleId + ": No R1 found for library: " + libId)

        addSummaryQScript(mapping)
      }
    }

    val bamFile = createFile(".bam")
    val controls: List[String] = config("control", default = Nil)

    def addJobs(): Unit = {
      addPerLibJobs()
      val bamFiles = libraries.map(_._2.mapping.finalBamFile).toList
      if (bamFiles.length == 1) {
        add(Ln(qscript, bamFiles.head, bamFile))
        val oldIndex = new File(bamFiles.head.getAbsolutePath.stripSuffix(".bam") + ".bai")
        val newIndex = new File(bamFile.getAbsolutePath.stripSuffix(".bam") + ".bai")
        add(Ln(qscript, oldIndex, newIndex))
      } else if (bamFiles.length > 1) {
        val merge = new MergeSamFiles(qscript)
        merge.input = bamFiles
        merge.sortOrder = "coordinate"
        merge.output = bamFile
        add(merge)
      }

      val bamMetrics = BamMetrics(qscript, bamFile, new File(sampleDir, "metrics"))
      addAll(bamMetrics.functions)
      addSummaryQScript(bamMetrics)
      addAll(Bam2Wig(qscript, bamFile).functions)

      val macs2 = new Macs2CallPeak(qscript)
      macs2.treatment = bamFile
      macs2.name = Some(sampleId)
      macs2.outputdir = sampleDir + File.separator + "macs2" + File.separator + sampleId + File.separator
      add(macs2)

      addSummaryJobs()
    }
  }

  override def reportClass = {
    val carp = new CarpReport(this)
    carp.outputDir = new File(outputDir, "report")
    carp.summaryFile = summaryFile
    Some(carp)
  }

  def init() = {
    // ensure that no samples are called 'control' since that is our reserved keyword
    require(!sampleIds.contains("control"),
      "No sample should be named 'control' since it is a reserved for the Carp pipeline")
  }

  def biopetScript() {
    // Define what the pipeline should do
    // First step is QC, this will be done with Flexiprep
    // Second step is mapping, this will be done with the Mapping pipeline
    // Third step is calling peaks on the bam files produced with the mapping pipeline, this will be done with MACS2
    logger.info("Starting CArP pipeline")

    addSamplesJobs()
  }

  def addMultiSampleJobs(): Unit = {
    for ((sampleId, sample) <- samples) {
      for (controlId <- sample.controls) {
        if (!samples.contains(controlId))
          throw new IllegalStateException("For sample: " + sampleId + " this control: " + controlId + " does not exist")
        val macs2 = new Macs2CallPeak(this)
        macs2.treatment = sample.bamFile
        macs2.control = samples(controlId).bamFile
        macs2.name = Some(sampleId + "_VS_" + controlId)
        macs2.outputdir = sample.sampleDir + File.separator + "macs2" + File.separator + macs2.name.get + File.separator
        add(macs2)
      }
    }
  }
}

object Carp extends PipelineCommand
