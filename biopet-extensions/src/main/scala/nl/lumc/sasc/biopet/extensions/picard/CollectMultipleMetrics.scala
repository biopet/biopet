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
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.summary.{Summarizable, SummaryQScript}
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * Extension for piacrd's CollectMultipleMetrics tool
  *
  * Created by pjvan_thof on 4/16/15.
  */
class CollectMultipleMetrics(val parent: Configurable)
    extends Picard
    with Summarizable
    with Reference {
  import CollectMultipleMetrics._

  override def defaultCoreMemory = 8.0

  @Input(doc = "The input SAM or BAM files to analyze", required = true)
  var input: File = _

  @Input(doc = "The reference file for the bam files.", shortName = "R")
  var reference: File = _

  @Output(doc = "Base name of output files", required = true)
  var outputName: File = _

  @Argument(doc = "Base name of output files", required = true)
  var program: List[Programs.Value] = {
    val value: List[String] = config("metrics_programs")
    value match {
      case Nil => Programs.values.toList
      case list => list.flatMap(x => Programs.values.find(_.toString.toLowerCase == x.toLowerCase))
    }
  }

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
      case Programs.CollectAlignmentSummaryMetrics =>
        outputFiles :+= new File(outputName + ".alignment_summary_metrics")
      case Programs.CollectInsertSizeMetrics =>
        outputFiles :+= new File(outputName + ".insert_size_metrics")
        outputFiles :+= new File(outputName + ".insert_size_histogram.pdf")
      case Programs.QualityScoreDistribution =>
        outputFiles :+= new File(outputName + ".quality_distribution_metrics")
        outputFiles :+= new File(outputName + ".test.quality_distribution.pdf")
      case Programs.MeanQualityByCycle =>
        outputFiles :+= new File(outputName + ".quality_by_cycle_metrics")
        outputFiles :+= new File(outputName + ".quality_by_cycle.pdf")
      case Programs.CollectBaseDistributionByCycle =>
        outputFiles :+= new File(outputName + ".base_distribution_by_cycle_metrics")
        outputFiles :+= new File(outputName + ".base_distribution_by_cycle.pdf")
      case p => Logging.addError("Program '" + p + "' does not exist for 'CollectMultipleMetrics'")
    }
  }

  override def cmdLine: String =
    super.cmdLine +
      required("INPUT=", input, spaceSeparated = false) +
      required("OUTPUT=", outputName, spaceSeparated = false) +
      conditional(assumeSorted, "ASSUME_SORTED=true") +
      optional("STOP_AFTER=", stopAfter, spaceSeparated = false) +
      optional("REFERENCE_SEQUENCE=", reference, spaceSeparated = false) +
      repeat("PROGRAM=", program.map(_.toString), spaceSeparated = false)

  override def addToQscriptSummary(qscript: SummaryQScript): Unit = {

    def summarizable(stats: () => Any): Summarizable = new Summarizable {
      def summaryStats: Any = stats()
      def summaryFiles: Map[String, File] = Map()
    }

    program
      .foreach {
        case Programs.CollectAlignmentSummaryMetrics =>
          qscript.addSummarizable(
            summarizable(
              () =>
                Picard.getMetrics(new File(outputName + ".alignment_summary_metrics"),
                  groupBy = Some("CATEGORY"))),
            Programs.CollectAlignmentSummaryMetrics.toString,
            forceSingle = true)
        case Programs.CollectInsertSizeMetrics =>
          qscript.addSummarizable(
            summarizable(
              () =>
                if (new File(outputName + ".insert_size_metrics").exists())
                  Map(
                    "metrics" -> Picard.getMetrics(
                      new File(outputName + ".insert_size_metrics")),
                    "histogram" -> Picard.getHistogram(
                      new File(outputName + ".insert_size_metrics"))
                  )
                else Map()),
            Programs.CollectInsertSizeMetrics.toString,
            forceSingle = true
          )
        case Programs.QualityScoreDistribution =>
          qscript.addSummarizable(
            summarizable(
              () => Picard.getHistogram(new File(outputName + ".quality_distribution_metrics"))),
            Programs.QualityScoreDistribution.toString,
            forceSingle = true)
        case Programs.MeanQualityByCycle =>
          qscript.addSummarizable(
            summarizable(
              () => Picard.getHistogram(new File(outputName + ".quality_by_cycle_metrics"))),
            Programs.MeanQualityByCycle.toString,
            forceSingle = true)
        case Programs.CollectBaseDistributionByCycle =>
          qscript.addSummarizable(
            summarizable(
              () =>
                Picard.getHistogram(new File(outputName + ".base_distribution_by_cycle_metrics"),
                  tag = "METRICS CLASS")),
            Programs.CollectBaseDistributionByCycle.toString,
            forceSingle = true)
        case _ => None
      }
  }

  def summaryStats = Map()

  def summaryFiles: Map[String, File] = {
    program
      .map {
        case Programs.CollectInsertSizeMetrics =>
          Map("insert_size_histogram" -> new File(outputName + ".insert_size_histogram.pdf"),
              "insert_size_metrics" -> new File(outputName + ".insert_size_metrics"))
        case _ => Map()
      }
      .foldLeft(Map.empty[String, File]) { case (acc, m) => acc ++ m }
  }
}

object CollectMultipleMetrics {
  object Programs extends Enumeration {
    val CollectAlignmentSummaryMetrics, CollectInsertSizeMetrics, QualityScoreDistribution,
    MeanQualityByCycle, CollectBaseDistributionByCycle = Value
  }
}
