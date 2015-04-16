package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Argument, Input }

/**
 * Created by pjvan_thof on 4/16/15.
 */
class CollectMultipleMetrics(val root: Configurable) extends Picard {
  import CollectMultipleMetrics._

  javaMainClass = new picard.analysis.CollectMultipleMetrics().getClass.getName

  @Input(doc = "The input SAM or BAM files to analyze", required = true)
  var input: File = null

  @Output(doc = "Base name of output files", required = true)
  var outputName: File = null

  @Argument(doc = "Base name of output files", required = true)
  var program: List[String] = config("metrics_programs",
    default = Programs.values.iterator.toList.map(_.toString))

  @Argument(doc = "Assume alignment file is sorted by position", required = false)
  var assumeSorted: Boolean = config("assume_sorted", default = false)

  @Argument(doc = "Stop after processing N reads", required = false)
  var stopAfter: Option[Long] = config("stop_after")

  @Output
  protected var outputFiles: List[File] = Nil

  override def beforeGraph: Unit = {
    program.foreach(p => p match {
      case _ if p == Programs.CollectAlignmentSummaryMetrics.toString => {
        outputFiles :+= new File(outputName + ".alignment_summary_metrics")
      }
      case _ if p == Programs.CollectInsertSizeMetrics.toString => {
        outputFiles :+= new File(outputName + ".insert_size_metrics")
        outputFiles :+= new File(outputName + ".insert_size_Histogram.pdf")
      }
      case _ if p == Programs.QualityScoreDistribution.toString => {
        outputFiles :+= new File(outputName + ".quality_distribution_metrics")
        outputFiles :+= new File(outputName + ".test.quality_distribution.pdf")
      }
      case _ if p == Programs.MeanQualityByCycle.toString => {
        outputFiles :+= new File(outputName + ".quality_by_cycle_metrics")
        outputFiles :+= new File(outputName + ".quality_by_cycle.pdf")
      }
      case _ if p == Programs.CollectBaseDistributionByCycle.toString => {
        outputFiles :+= new File(outputName + ".base_distribution_by_cycle_metrics")
        outputFiles :+= new File(outputName + ".base_distribution_by_cycle.pdf")
      }
      case _ => BiopetQScript.addError("Program '" + p + "' does not exist for 'CollectMultipleMetrics'")
    })
  }

  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", outputName, spaceSeparated = false) +
    conditional(assumeSorted, "ASSUME_SORTED=true") +
    optional("STOP_AFTER=", stopAfter, spaceSeparated = false) +
    repeat("PROGRAM=", program, spaceSeparated = false)
}

object CollectMultipleMetrics {
  object Programs extends Enumeration {
    val CollectAlignmentSummaryMetrics, CollectInsertSizeMetrics, QualityScoreDistribution, MeanQualityByCycle, CollectBaseDistributionByCycle = Value
  }
}