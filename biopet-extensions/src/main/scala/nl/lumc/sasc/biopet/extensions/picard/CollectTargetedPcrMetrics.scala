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

/**
  * Extension for piacrd's CollectTargetedPcrMetrics
  *
  * Created by pjvan_thof on 4/16/15.
  */
class CollectTargetedPcrMetrics(val parent: Configurable)
    extends Picard
    with Summarizable
    with Reference {

  @Input(doc = "The input SAM or BAM files to analyze.  Must be coordinate sorted.",
         required = true)
  var input: File = _

  @Input(doc = "Reference", required = true)
  var reference: File = null

  @Input(doc = "AMPLICON_INTERVALS", required = true)
  var ampliconIntervals: File = _

  @Input(doc = "TARGET_INTERVALS", required = true)
  var targetIntervals: List[File] = Nil

  @Output(doc = "The output file to write statistics to", required = true)
  var output: File = _

  @Output(doc = "PER_TARGET_COVERAGE", required = false)
  var perTargetCoverage: File = _

  @Argument(doc = "METRIC_ACCUMULATION_LEVEL", required = false)
  var metricAccumulationLevel: List[String] = config("metricaccumulationlevel", default = Nil)

  @Argument(doc = "CUSTOM_AMPLICON_SET_NAME", required = false)
  var customAmpliconSetName: Option[String] = config("custom_amplicon_set_name")

  override def beforeGraph() {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
  }

  override def cmdLine =
    super.cmdLine +
      required("INPUT=", input, spaceSeparated = false) +
      required("OUTPUT=", output, spaceSeparated = false) +
      required("REFERENCE_SEQUENCE=", reference, spaceSeparated = false) +
      repeat("METRIC_ACCUMULATION_LEVEL=", metricAccumulationLevel, spaceSeparated = false) +
      required("AMPLICON_INTERVALS=", ampliconIntervals, spaceSeparated = false) +
      repeat("TARGET_INTERVALS=", targetIntervals, spaceSeparated = false) +
      optional("PER_TARGET_COVERAGE=", perTargetCoverage, spaceSeparated = false) +
      optional("CUSTOM_AMPLICON_SET_NAME=", customAmpliconSetName, spaceSeparated = false)

  /** Returns files for summary */
  def summaryFiles: Map[String, File] = Map()

  /** Returns stats for summary */
  def summaryStats = Picard.getMetrics(output).getOrElse(Map())
}

object CollectTargetedPcrMetrics {
  def apply(root: Configurable,
            input: File,
            ampliconIntervals: File,
            targetIntervals: List[File],
            outputDir: File): CollectTargetedPcrMetrics = {
    val pcrMetrics = new CollectTargetedPcrMetrics(root)
    pcrMetrics.input = input
    pcrMetrics.ampliconIntervals = ampliconIntervals
    pcrMetrics.targetIntervals = targetIntervals
    pcrMetrics.output =
      new File(outputDir, input.getName.stripSuffix(".bam") + ".TargetedPcrMetrics")
    pcrMetrics.perTargetCoverage = new File(
      outputDir,
      input.getName.stripSuffix(".bam") + ".TargetedPcrMetrics.per_target_coverage")
    pcrMetrics
  }
}
