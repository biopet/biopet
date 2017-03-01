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

import nl.lumc.sasc.biopet.core.{ Reference, BiopetQScript }
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.summary.{ Summarizable, SummaryQScript }
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Extension for piacrd's CollectMultipleMetrics tool
 *
 * Created by pjvan_thof on 4/16/15.
 */
class CollectMultipleMetrics(val parent: Configurable) extends Picard with Summarizable with Reference {
  import CollectMultipleMetrics._

  javaMainClass = new picard.analysis.CollectMultipleMetrics().getClass.getName

  override def defaultCoreMemory = 8.0

  @Input(doc = "The input SAM or BAM files to analyze", required = true)
  var input: File = null

  @Input(doc = "The reference file for the bam files.", shortName = "R")
  var reference: File = null

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
  private var outputFiles: List[File] = Nil

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
    program.foreach {
      case p if p == Programs.CollectAlignmentSummaryMetrics.toString =>
        outputFiles :+= new File(outputName + ".alignment_summary_metrics")
      case p if p == Programs.CollectInsertSizeMetrics.toString =>
        outputFiles :+= new File(outputName + ".insert_size_metrics")
        outputFiles :+= new File(outputName + ".insert_size_histogram.pdf")
      case p if p == Programs.QualityScoreDistribution.toString =>
        outputFiles :+= new File(outputName + ".quality_distribution_metrics")
        outputFiles :+= new File(outputName + ".test.quality_distribution.pdf")
      case p if p == Programs.MeanQualityByCycle.toString =>
        outputFiles :+= new File(outputName + ".quality_by_cycle_metrics")
        outputFiles :+= new File(outputName + ".quality_by_cycle.pdf")
      case p if p == Programs.CollectBaseDistributionByCycle.toString =>
        outputFiles :+= new File(outputName + ".base_distribution_by_cycle_metrics")
        outputFiles :+= new File(outputName + ".base_distribution_by_cycle.pdf")
      case p => Logging.addError("Program '" + p + "' does not exist for 'CollectMultipleMetrics'")
    }
  }

  override def cmdLine = super.cmdLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", outputName, spaceSeparated = false) +
    conditional(assumeSorted, "ASSUME_SORTED=true") +
    optional("STOP_AFTER=", stopAfter, spaceSeparated = false) +
    optional("REFERENCE_SEQUENCE=", reference, spaceSeparated = false) +
    repeat("PROGRAM=", program, spaceSeparated = false)

  override def addToQscriptSummary(qscript: SummaryQScript): Unit = {

    def summarizable(stats: () => Any): Summarizable = new Summarizable {
      def summaryStats = stats()
      def summaryFiles: Map[String, File] = Map()
    }

    program
      .foreach { p =>
        p match {
          case _ if p == Programs.CollectAlignmentSummaryMetrics.toString =>
            qscript.addSummarizable(summarizable(() => Picard.getMetrics(new File(outputName + ".alignment_summary_metrics"), groupBy = Some("CATEGORY"))), p, forceSingle = true)
          case _ if p == Programs.CollectInsertSizeMetrics.toString =>
            qscript.addSummarizable(summarizable(() => if (new File(outputName + ".insert_size_metrics").exists()) Map(
              "metrics" -> Picard.getMetrics(new File(outputName + ".insert_size_metrics")),
              "histogram" -> Picard.getHistogram(new File(outputName + ".insert_size_metrics"))
            )
            else Map()), p, forceSingle = true)
          case _ if p == Programs.QualityScoreDistribution.toString =>
            qscript.addSummarizable(summarizable(() => Picard.getHistogram(new File(outputName + ".quality_distribution_metrics"))), p, forceSingle = true)
          case _ if p == Programs.MeanQualityByCycle.toString =>
            qscript.addSummarizable(summarizable(() => Picard.getHistogram(new File(outputName + ".quality_by_cycle_metrics"))), p, forceSingle = true)
          case _ if p == Programs.CollectBaseDistributionByCycle.toString =>
            qscript.addSummarizable(summarizable(() => Picard.getHistogram(new File(outputName + ".base_distribution_by_cycle_metrics"), tag = "METRICS CLASS")), p, forceSingle = true)
          case _ => None
        }
      }
  }

  def summaryStats = Map()

  def summaryFiles: Map[String, File] = {
    program.map {
      case p if p == Programs.CollectInsertSizeMetrics.toString =>
        Map(
          "insert_size_histogram" -> new File(outputName + ".insert_size_histogram.pdf"),
          "insert_size_metrics" -> new File(outputName + ".insert_size_metrics"))
      case otherwise => Map()
    }.foldLeft(Map.empty[String, File]) { case (acc, m) => acc ++ m }
  }
}

object CollectMultipleMetrics {
  object Programs extends Enumeration {
    val CollectAlignmentSummaryMetrics, CollectInsertSizeMetrics, QualityScoreDistribution, MeanQualityByCycle, CollectBaseDistributionByCycle = Value
  }
}