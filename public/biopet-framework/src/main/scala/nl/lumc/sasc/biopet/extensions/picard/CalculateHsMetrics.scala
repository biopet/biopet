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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

class CalculateHsMetrics(val root: Configurable) extends Picard {
  javaMainClass = "picard.analysis.directed.CalculateHsMetrics"

  @Input(doc = "The input SAM or BAM files to analyze.  Must be coordinate sorted.", required = true)
  var input: File = _

  @Input(doc = "BAIT_INTERVALS", required = true)
  var baitIntervals: File = _

  @Input(doc = "TARGET_INTERVALS", required = true)
  var targetIntervals: File = _

  @Output(doc = "The output file to write statistics to", required = true)
  var output: File = _

  @Output(doc = "PER_TARGET_COVERAGE", required = false)
  var perTargetCoverage: File = _

  @Argument(doc = "Reference file", required = false)
  var reference: File = config("reference")

  @Argument(doc = "METRIC_ACCUMULATION_LEVEL", required = false)
  var metricAccumulationLevel: List[String] = config("metricaccumulationlevel")

  @Argument(doc = "BAIT_SET_NAME", required = false)
  var baitSetName: String = _

  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    optional("REFERENCE_SEQUENCE=", reference, spaceSeparated = false) +
    repeat("METRIC_ACCUMULATION_LEVEL=", metricAccumulationLevel, spaceSeparated = false) +
    required("BAIT_INTERVALS=", baitIntervals, spaceSeparated = false) +
    required("TARGET_INTERVALS=", targetIntervals, spaceSeparated = false) +
    optional("PER_TARGET_COVERAGE=", perTargetCoverage, spaceSeparated = false) +
    optional("BAIT_SET_NAME=", baitSetName, spaceSeparated = false)
}

object CalculateHsMetrics {
  def apply(root: Configurable, input: File, baitIntervals: File, targetIntervals: File, outputDir: File): CalculateHsMetrics = {
    val calculateHsMetrics = new CalculateHsMetrics(root)
    calculateHsMetrics.input = input
    calculateHsMetrics.baitIntervals = baitIntervals
    calculateHsMetrics.targetIntervals = targetIntervals
    calculateHsMetrics.output = new File(outputDir, input.getName.stripSuffix(".bam") + ".capmetrics")
    calculateHsMetrics.perTargetCoverage = new File(outputDir, input.getName.stripSuffix(".bam") + ".per_target_coverage")
    return calculateHsMetrics
  }
}