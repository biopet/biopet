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

/** Extension for picard CollectAlignmentSummaryMetrics */
class CollectAlignmentSummaryMetrics(val parent: Configurable)
    extends Picard
    with Summarizable
    with Reference {
  javaMainClass = new picard.analysis.CollectAlignmentSummaryMetrics().getClass.getName

  @Input(doc = "The input SAM or BAM files to analyze.  Must be coordinate sorted.",
         required = true)
  var input: File = _

  @Argument(doc = "MAX_INSERT_SIZE", required = false)
  var maxInstertSize: Option[Int] = config("maxInstertSize")

  @Argument(doc = "ADAPTER_SEQUENCE", required = false)
  var adapterSequence: List[String] = config("adapterSequence", default = Nil)

  @Argument(doc = "IS_BISULFITE_SEQUENCED", required = false)
  var isBisulfiteSequenced: Option[Boolean] = config("isBisulfiteSequenced")

  @Output(doc = "The output file to write statistics to", required = true)
  var output: File = _

  @Argument(doc = "Reference file", required = false)
  var reference: File = _

  @Argument(doc = "ASSUME_SORTED", required = false)
  var assumeSorted: Boolean = config("assumeSorted", default = true)

  @Argument(doc = "METRIC_ACCUMULATION_LEVEL", required = false)
  var metricAccumulationLevel: List[String] = config("metricaccumulationlevel", default = Nil)

  @Argument(doc = "STOP_AFTER", required = false)
  var stopAfter: Option[Long] = config("stopAfter")

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
  }

  /** Returns command to execute */
  override def cmdLine =
    super.cmdLine +
      required("INPUT=", input, spaceSeparated = false) +
      required("OUTPUT=", output, spaceSeparated = false) +
      optional("REFERENCE_SEQUENCE=", reference, spaceSeparated = false) +
      repeat("METRIC_ACCUMULATION_LEVEL=", metricAccumulationLevel, spaceSeparated = false) +
      optional("MAX_INSERT_SIZE=", maxInstertSize, spaceSeparated = false) +
      optional("IS_BISULFITE_SEQUENCED=", isBisulfiteSequenced, spaceSeparated = false) +
      optional("ASSUME_SORTED=", assumeSorted, spaceSeparated = false) +
      optional("STOP_AFTER=", stopAfter, spaceSeparated = false) +
      repeat("ADAPTER_SEQUENCE=", adapterSequence, spaceSeparated = false)

  def summaryFiles: Map[String, File] = Map()

  /** Returns stats for summary */
  def summaryStats = Picard.getMetrics(output).getOrElse(Map())
}

object CollectAlignmentSummaryMetrics {

  /** Returns default CollectAlignmentSummaryMetrics */
  def apply(root: Configurable, input: File, outputDir: File): CollectAlignmentSummaryMetrics = {
    val collectAlignmentSummaryMetrics = new CollectAlignmentSummaryMetrics(root)
    collectAlignmentSummaryMetrics.input = input
    collectAlignmentSummaryMetrics.output =
      new File(outputDir, input.getName.stripSuffix(".bam") + ".alignmentMetrics")
    collectAlignmentSummaryMetrics
  }
}
