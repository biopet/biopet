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
package nl.lumc.sasc.biopet.pipelines.carp

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.report.ReportBuilderExtension
import nl.lumc.sasc.biopet.extensions.picard.BuildBamIndex
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsView
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.bamtobigwig.Bam2Wig
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingTrait
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config._
import org.broadinstitute.gatk.queue.QScript

/**
  * Carp pipeline
  * Chip-Seq analysis pipeline
  * This pipeline performs QC,mapping and peak calling
  */
class Carp(val parent: Configurable) extends QScript with MultisampleMappingTrait with Reference {
  qscript =>
  def this() = this(null)

  override def defaults: Map[String, Any] = super.defaults ++ Map(
    "mapping" -> Map(
      "skip_markduplicates" -> false,
      "aligner" -> "bwa-mem"
    ),
    "merge_strategy" -> "preprocessmergesam",
    "samtoolsview" -> Map("q" -> 10)
  )

  override def fixedValues: Map[String, Any] = super.fixedValues ++ Map(
    "samtoolsview" -> Map(
      "h" -> true,
      "b" -> true
    ),
    "macs2callpeak" -> Map("fileformat" -> "")
  )

  override def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends super.Sample(sampleId) {

    override def bamFile: Option[File] =
      if (libraries.size == 1) libraries.head._2.mapping.map(_.finalBamFile) else super.bamFile

    override def preProcessBam = Some(createFile("filter.bam"))

    override def metricsPreprogressBam = false

    val controls: List[String] = config("control", default = Nil)

    override def summarySettings: Map[String, Any] =
      super.summarySettings ++ Map("controls" -> controls)

    override def addJobs(): Unit = {
      super.addJobs()

      add(Bam2Wig(qscript, bamFile.get))

      val samtoolsView = new SamtoolsView(qscript)
      samtoolsView.input = bamFile.get
      samtoolsView.output = preProcessBam.get
      samtoolsView.b = true
      samtoolsView.h = true
      add(samtoolsView)

      val bamMetricsFilter = BamMetrics(qscript,
                                        preProcessBam.get,
                                        new File(sampleDir, "metrics-filter"),
                                        paired,
                                        sampleId = Some(sampleId))
      addAll(bamMetricsFilter.functions)
      bamMetricsFilter.summaryName = "bammetrics-filter"
      addSummaryQScript(bamMetricsFilter)

      val buildBamIndex = new BuildBamIndex(qscript)
      buildBamIndex.input = preProcessBam.get
      buildBamIndex.output =
        swapExt(preProcessBam.get.getParentFile, preProcessBam.get, ".bam", ".bai")
      add(buildBamIndex)
    }
  }

  override def reportClass: Option[ReportBuilderExtension] = {
    val carp = new CarpReport(this)
    carp.outputDir = new File(outputDir, "report")
    carp.summaryDbFile = summaryDbFile
    Some(carp)
  }

  lazy val paired: Boolean = {
    val notPaired = samples.forall(_._2.libraries.forall(_._2.inputR2.isEmpty))
    val p = samples.forall(_._2.libraries.forall(_._2.inputR2.isDefined))
    if (!notPaired && !p)
      Logging.addError(
        "Combination of Paired-end and Single-end detected, this is not allowed in Carp")
    p
  }

  override def init(): Unit = {
    super.init()
    // ensure that no samples are called 'control' since that is our reserved keyword
    require(!sampleIds.contains("control"),
            "No sample should be named 'control' since it is a reserved for the Carp pipeline")
    peakCalling.controls = samples.map(x => x._1 -> x._2.controls)
    peakCalling.outputDir = new File(outputDir, "peak_calling")
  }

  lazy val peakCalling = new PeakCalling(this)

  override def addMultiSampleJobs(): Unit = {
    super.addMultiSampleJobs()
    peakCalling.inputBams = samples.map(x => x._1 -> x._2.preProcessBam.get)
    peakCalling.paired = paired
    add(peakCalling)
  }
}

object Carp extends PipelineCommand
