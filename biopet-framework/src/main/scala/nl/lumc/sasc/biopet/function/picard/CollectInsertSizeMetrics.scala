package nl.lumc.sasc.biopet.function.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output, Argument}

class CollectInsertSizeMetrics(val root:Configurable) extends Picard {
  javaMainClass = "picard.analysis.CollectInsertSizeMetrics"

  @Input(doc="The input SAM or BAM files to analyze.  Must be coordinate sorted.", required = true)
  var input: File = _

  @Output(doc="The output file to write statistics to", required = true)
  var output: File = _
  
  @Output(doc="Output histogram", required = true)
  var outputHistogram: File = _
  
  @Argument(doc="Reference file", required = false)
  var reference: File = config("reference")
  
  @Argument(doc="DEVIATIONS", required = false)
  var deviations: Option[Double] = config("deviations")
  
  @Argument(doc="MINIMUM_PCT", required=false)
  var minPct: Option[Double] = config("minpct")
  
  @Argument(doc="ASSUME_SORTED", required=false)
  var assumeSorted: Boolean = config("assumesorted", default=true)
  
  @Argument(doc="STOP_AFTER", required=false)
  var stopAfter: Option[Long] = config("stopAfter")
  
  @Argument(doc="METRIC_ACCUMULATION_LEVEL", required=false)
  var metricAccumulationLevel: List[String] = config("metricaccumulationlevel")
  
  @Argument(doc="HISTOGRAM_WIDTH", required=false)
  var histogramWidth: Option[Int] = config("histogramWidth")
  
  override def afterGraph {
    if (outputHistogram == null) outputHistogram = new File(output + ".pdf")
    //require(reference.exists)
  }
  
  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated=false) +
    required("OUTPUT=", output, spaceSeparated=false) +
    optional("HISTOGRAM_FILE=", outputHistogram, spaceSeparated=false) +
    required("REFERENCE_SEQUENCE=", reference, spaceSeparated=false) + 
    optional("DEVIATIONS=", deviations, spaceSeparated=false) +
    repeat("METRIC_ACCUMULATION_LEVEL=", metricAccumulationLevel, spaceSeparated=false) +
    optional("STOP_AFTER=", stopAfter, spaceSeparated=false) +
    optional("HISTOGRAM_WIDTH=", histogramWidth, spaceSeparated=false) +
    conditional(assumeSorted, "ASSUME_SORTED=TRUE")
}

object CollectInsertSizeMetrics {
  def apply(root:Configurable, input:File, outputDir:String) : CollectInsertSizeMetrics = {
    val collectInsertSizeMetrics = new CollectInsertSizeMetrics(root)
    collectInsertSizeMetrics.input = input
    collectInsertSizeMetrics.output = new File(outputDir, input.getName.stripSuffix(".bam") + ".insertsizemetrics")
    return collectInsertSizeMetrics
  }
}