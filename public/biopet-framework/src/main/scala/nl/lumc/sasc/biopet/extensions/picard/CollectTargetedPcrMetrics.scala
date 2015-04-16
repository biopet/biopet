package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output, Input }

/**
 * Created by pjvan_thof on 4/16/15.
 */
class CollectTargetedPcrMetrics(val root: Configurable) extends Picard {

  javaMainClass = new picard.analysis.directed.CollectTargetedPcrMetrics().getClass.getName

  @Input(doc = "The input SAM or BAM files to analyze.  Must be coordinate sorted.", required = true)
  var input: File = _

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

  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    repeat("METRIC_ACCUMULATION_LEVEL=", metricAccumulationLevel, spaceSeparated = false) +
    required("AMPLICON_INTERVALS=", ampliconIntervals, spaceSeparated = false) +
    repeat("TARGET_INTERVALS=", targetIntervals, spaceSeparated = false) +
    optional("PER_TARGET_COVERAGE=", perTargetCoverage, spaceSeparated = false) +
    optional("CUSTOM_AMPLICON_SET_NAME=", customAmpliconSetName, spaceSeparated = false)
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
    pcrMetrics.output = new File(outputDir, input.getName.stripSuffix(".bam") + ".TargetedPcrMetrics")
    pcrMetrics.perTargetCoverage = new File(outputDir, input.getName.stripSuffix(".bam") + ".TargetedPcrMetrics.per_target_coverage")
    pcrMetrics
  }
}