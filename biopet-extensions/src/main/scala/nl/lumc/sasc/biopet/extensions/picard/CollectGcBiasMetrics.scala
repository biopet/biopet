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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.summary.Summarizable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/** Extension for picard CollectGcBiasMetrics */
class CollectGcBiasMetrics(val parent: Configurable)
    extends Picard
    with Summarizable
    with Reference {

  @Input(doc = "The input SAM or BAM files to analyze.  Must be coordinate sorted.",
         required = true)
  var input: Seq[File] = Nil

  @Output(doc = "The output file to write statistics to", required = true)
  var output: File = _

  @Output(doc = "Output chart", required = false)
  var outputChart: File = _

  @Output(doc = "Output summary", required = true)
  var outputSummary: File = _

  @Input(doc = "Reference file", required = false)
  var reference: File = null

  @Argument(doc = "Window size", required = false)
  var windowSize: Option[Int] = config("windowsize")

  @Argument(doc = "MINIMUM_GENOME_FRACTION", required = false)
  var minGenomeFraction: Option[Double] = config("mingenomefraction")

  @Argument(doc = "ASSUME_SORTED", required = false)
  var assumeSorted: Boolean = config("assumesorted", default = true)

  @Argument(doc = "IS_BISULFITE_SEQUENCED", required = false)
  var isBisulfiteSequinced: Option[Boolean] = config("isbisulfitesequinced")

  override def defaultCoreMemory = 8.0

  override def beforeGraph() {
    super.beforeGraph()
    if (outputChart == null) outputChart = new File(output + ".pdf")
    if (reference == null) reference = referenceFasta()
  }

  /** Returns command to execute */
  override def cmdLine =
    super.cmdLine +
      repeat("INPUT=", input, spaceSeparated = false) +
      required("OUTPUT=", output, spaceSeparated = false) +
      optional("CHART_OUTPUT=", outputChart, spaceSeparated = false) +
      required("REFERENCE_SEQUENCE=", reference, spaceSeparated = false) +
      required("SUMMARY_OUTPUT=", outputSummary, spaceSeparated = false) +
      optional("WINDOW_SIZE=", windowSize, spaceSeparated = false) +
      optional("MINIMUM_GENOME_FRACTION=", minGenomeFraction, spaceSeparated = false) +
      conditional(assumeSorted, "ASSUME_SORTED=TRUE") +
      conditional(isBisulfiteSequinced.getOrElse(false), "IS_BISULFITE_SEQUENCED=TRUE")

  /** Returns files for summary */
  def summaryFiles: Map[String, File] = Map()

  /** Returns stats for summary */
  def summaryStats = Picard.getHistogram(output, tag = "METRICS CLASS")
}

object CollectGcBiasMetrics {

  /** Returns default CollectGcBiasMetrics */
  def apply(root: Configurable, input: File, outputDir: File): CollectGcBiasMetrics = {
    val collectGcBiasMetrics = new CollectGcBiasMetrics(root)
    collectGcBiasMetrics.input :+= input
    collectGcBiasMetrics.output =
      new File(outputDir, input.getName.stripSuffix(".bam") + ".gcbiasmetrics")
    collectGcBiasMetrics.outputSummary =
      new File(outputDir, input.getName.stripSuffix(".bam") + ".gcbiasmetrics.summary")
    collectGcBiasMetrics
  }
}
