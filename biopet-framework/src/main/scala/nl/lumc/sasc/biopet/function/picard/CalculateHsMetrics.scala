package nl.lumc.sasc.biopet.function.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.sting.commandline.{Argument, Input, Output}

class CalculateHsMetrics(val root:Configurable) extends Picard {
  javaMainClass = "net.sf.picard.analysis.directed.CalculateHsMetrics"
  
  @Input(doc="The input SAM or BAM files to analyze.  Must be coordinate sorted.", required = true)
  var input: File = _
  
  @Input(doc="BAIT_INTERVALS", required = true)
  var baitIntervals: File = _
  
  @Input(doc="TARGET_INTERVALS", required = true)
  var targetIntervals: File = _
  
  @Output(doc="The output file to write statistics to", required = true)
  var output: File = _
  
  @Output(doc="PER_TARGET_COVERAGE", required = false)
  var perTargetCoverage: File = _
  
  @Argument(doc="Reference file", required = false)
  var reference: File = config("reference", "")
  
  @Argument(doc="METRIC_ACCUMULATION_LEVEL", required=false)
  var metricAccumulationLevel: List[String] = config("metricaccumulationlevel", List())
  
  @Argument(doc="BAIT_SET_NAME", required = false)
  var baitSetName: String = _
  
  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated=false) +
    required("OUTPUT=", output, spaceSeparated=false) +
    optional("REFERENCE_SEQUENCE=", reference, spaceSeparated=false) + 
    repeat("METRIC_ACCUMULATION_LEVEL=", metricAccumulationLevel, spaceSeparated=false) +
    required("BAIT_INTERVALS=", baitIntervals, spaceSeparated=false) + 
    required("TARGET_INTERVALS=", targetIntervals, spaceSeparated=false) +
    optional("PER_TARGET_COVERAGE=", perTargetCoverage, spaceSeparated=false) +
    optional("BAIT_SET_NAME=", baitSetName, spaceSeparated=false)
}

object CalculateHsMetrics {
  def apply(root:Configurable, input:File, baitIntervals:File, targetIntervals:File, outputDir:String) : CalculateHsMetrics = {
    val calculateHsMetrics = new CalculateHsMetrics(root)
    calculateHsMetrics.input = input
    calculateHsMetrics.baitIntervals = baitIntervals
    calculateHsMetrics.targetIntervals = targetIntervals
    calculateHsMetrics.output = new File(outputDir, input.getName.stripSuffix(".bam") + ".capmetrics")
    calculateHsMetrics.perTargetCoverage = new File(outputDir, input.getName.stripSuffix(".bam") + ".per_target_coverage")
    return calculateHsMetrics
  }
}