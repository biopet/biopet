package nl.lumc.sasc.biopet.function.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output, Argument}

class CollectAlignmentSummaryMetrics(val root:Configurable) extends Picard {
  javaMainClass = "picard.analysis.CollectAlignmentSummaryMetrics"
  
  @Input(doc="The input SAM or BAM files to analyze.  Must be coordinate sorted.", required = true)
  var input: File = _
  
  @Argument(doc="MAX_INSERT_SIZE", required = false)
  var maxInstertSize: Int = config("maxInstertSize", 100000)
  
  @Argument(doc="ADAPTER_SEQUENCE", required = false)
  var adapterSequence: List[String] = config("adapterSequence", Nil)
  
  @Argument(doc="IS_BISULFITE_SEQUENCED", required = false)
  var isBisulfiteSequenced: Boolean = config("isBisulfiteSequenced", false)
  
  @Output(doc="The output file to write statistics to", required = true)
  var output: File = _
  
  @Argument(doc="Reference file", required = false)
  var reference: File = config("reference", "")
  
  @Argument(doc="ASSUME_SORTED", required = false)
  var assumeSorted: Boolean = config("assumeSorted", false)
  
  @Argument(doc="METRIC_ACCUMULATION_LEVEL", required=false)
  var metricAccumulationLevel: List[String] = config("metricaccumulationlevel", List())
  
  @Argument(doc="STOP_AFTER", required = false)
  var stopAfter: Long = config("stopAfter", 0)
  
  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated=false) +
    required("OUTPUT=", output, spaceSeparated=false) +
    optional("REFERENCE_SEQUENCE=", reference, spaceSeparated=false) + 
    repeat("METRIC_ACCUMULATION_LEVEL=", metricAccumulationLevel, spaceSeparated=false) +
    required("MAX_INSERT_SIZE=", maxInstertSize, spaceSeparated=false) + 
    required("IS_BISULFITE_SEQUENCED=", isBisulfiteSequenced, spaceSeparated=false) +
    optional("ASSUME_SORTED=", assumeSorted, spaceSeparated=false) +
    optional("STOP_AFTER=", stopAfter, spaceSeparated=false)
}

object CollectAlignmentSummaryMetrics {
  def apply(root:Configurable, input:File, outputDir:String) : CollectAlignmentSummaryMetrics = {
    val collectAlignmentSummaryMetrics = new CollectAlignmentSummaryMetrics(root)
    collectAlignmentSummaryMetrics.input = input
    collectAlignmentSummaryMetrics.output = new File(outputDir, input.getName.stripSuffix(".bam") + ".alignmentMetrics")
    return collectAlignmentSummaryMetrics
  }
}