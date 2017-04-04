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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.report.{ ReportBuilder, ReportBuilderExtension, ReportPage, ReportSection }
import nl.lumc.sasc.biopet.utils.rscript.StackedBarPlot
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

class FlexiprepReport(val parent: Configurable) extends ReportBuilderExtension {
  def builder = FlexiprepReport
}

/**
 * Class to generate a report for [[Flexiprep]]
 *
 * Created by pjvan_thof on 3/30/15.
 */
object FlexiprepReport extends ReportBuilder {
  val reportName = "Flexiprep"

  def pipelineName = "flexiprep"

  override def pageArgs = Map("multisample" -> false)

  /** Index page for a flexiprep report */
  def indexPage: Future[ReportPage] = Future {
    val flexiprepPage = this.flexiprepPage
    ReportPage(Nil, List(
      "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepFront.ssp")
    ) ::: Await.result(flexiprepPage, Duration.Inf).sections,
      Map()
    )
  }

  /** Generate a QC report page for 1 single library, sampleId and libId must be defined in the arguments */
  def flexiprepPage: Future[ReportPage] = Future(ReportPage(
    List(),
    List(
      "Read Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "Base Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp"),
      fastqcPlotSection("Base quality", "plot_per_base_quality"),
      fastqcPlotSection("Sequence quality", "plot_per_sequence_quality"),
      fastqcPlotSection("Base GC content", "plot_per_base_gc_content"),
      fastqcPlotSection("Sequence GC content", "plot_per_sequence_gc_content"),
      fastqcPlotSection("Base sequence content", "plot_per_base_sequence_content"),
      fastqcPlotSection("Duplication", "plot_duplication_levels"),
      fastqcPlotSection("Kmers", "plot_kmer_profiles"),
      fastqcPlotSection("Length distribution", "plot_sequence_length_distribution")
    ),
    Map()
  ))

  protected def fastqcPlotSection(name: String, tag: String) = {
    name -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepFastQcPlot.ssp", Map("plot" -> tag))
  }

  /**
   * Generated a stacked bar plot for reads QC
   * @param outputDir OutputDir for plot
   * @param prefix prefix for tsv and png file
   * @param read Must give "R1" or "R2"
   * @param summary Summary class
   * @param sampleId Default selects all samples, when given plot is limits on given sample
   */
  def readSummaryPlot(outputDir: File,
                      prefix: String,
                      read: String,
                      summary: SummaryDb,
                      sampleId: Option[Int] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    tsvWriter.println("Library\tAfter_QC\tClipping\tTrimming\tSynced")

    val seqstatPaths = Map("num_total" -> List("reads", "num_total"))
    val seqstatStats = summary.getStatsForLibraries(runId, "flexiprep", "seqstat_" + read, keyValues = seqstatPaths)
    val seqstatQcStats = summary.getStatsForLibraries(runId, "flexiprep", "seqstat_" + read + "_qc", keyValues = seqstatPaths)

    val clippingPaths = Map("num_reads_discarded_too_short" -> List("num_reads_discarded_too_short"),
      "num_reads_discarded_too_long" -> List("num_reads_discarded_too_long"))
    val clippingStats = summary.getStatsForLibraries(runId, "flexiprep", "clipping_" + read, keyValues = clippingPaths)

    val trimmingPaths = Map("num_reads_discarded" -> List("num_reads_discarded_" + read))
    val trimmingStats = summary.getStatsForLibraries(runId, "flexiprep", "trimming", keyValues = trimmingPaths)

    val libraries = Await.result(summary.getLibraries(runId = runId, sampleId = sampleId), Duration.Inf)

    for (lib <- libraries) {
      val beforeTotal = seqstatStats((lib.sampleId, lib.id))("num_total").getOrElse(0).toString.toLong
      val afterTotal = seqstatQcStats((lib.sampleId, lib.id))("num_total").getOrElse(0).toString.toLong
      val clippingDiscardedToShort = clippingStats((lib.sampleId, lib.id))("num_reads_discarded_too_short").getOrElse(0).toString.toLong
      val clippingDiscardedToLong = clippingStats((lib.sampleId, lib.id))("num_reads_discarded_too_long").getOrElse(0).toString.toLong
      val trimmingDiscarded = trimmingStats((lib.sampleId, lib.id))("num_reads_discarded").getOrElse(0).toString.toLong

      val sb = new StringBuffer()
      sb.append(Await.result(summary.getSampleName(lib.sampleId), Duration.Inf) + "-" + lib.name + "\t")
      sb.append(afterTotal + "\t")
      sb.append((clippingDiscardedToShort + clippingDiscardedToLong) + "\t")
      sb.append(trimmingDiscarded + "\t")
      sb.append(beforeTotal - afterTotal - trimmingDiscarded - clippingDiscardedToShort - clippingDiscardedToLong)

      tsvWriter.println(sb.toString)
    }

    tsvWriter.close()

    val plot = new StackedBarPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("Reads")
    plot.width = Some(200 + (libraries.filter(s => sampleId.getOrElse(s.id) == s.id).size * 10))
    plot.title = Some("QC summary on " + read + " reads")
    plot.runLocal()
  }

  /**
   * Generated a stacked bar plot for bases QC
   * @param outputDir OutputDir for plot
   * @param prefix prefix for tsv and png file
   * @param read Must give "R1" or "R2"
   * @param summary Summary class
   * @param sampleId Default selects all samples, when given plot is limits on given sample
   */
  def baseSummaryPlot(outputDir: File,
                      prefix: String,
                      read: String,
                      summary: SummaryDb,
                      sampleId: Option[Int] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    tsvWriter.println("Library\tAfter_QC\tDiscarded")

    val statsPaths = Map("num_total" -> List("bases", "num_total"))
    val seqstatStats = summary.getStatsForLibraries(runId, "flexiprep", "seqstat_" + read, keyValues = statsPaths)
    val seqstatQcStats = summary.getStatsForLibraries(runId, "flexiprep", "seqstat_" + read + "_qc", keyValues = statsPaths)

    val libraries = Await.result(summary.getLibraries(runId = runId, sampleId = sampleId), Duration.Inf)

    for (lib <- libraries) {
      val beforeTotal = seqstatStats((lib.sampleId, lib.id))("num_total").getOrElse(0).toString.toLong
      val afterTotal = seqstatQcStats((lib.sampleId, lib.id))("num_total").getOrElse(0).toString.toLong

      val sb = new StringBuffer()
      sb.append(Await.result(summary.getSampleName(lib.sampleId), Duration.Inf) + "-" + lib + "\t")
      sb.append(afterTotal + "\t")
      sb.append(beforeTotal - afterTotal)

      tsvWriter.println(sb.toString)
    }

    tsvWriter.close()

    val plot = new StackedBarPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("Bases")
    plot.width = Some(200 + (libraries.filter(s => sampleId.getOrElse(s.id) == s.id).size * 10))
    plot.title = Some("QC summary on " + read + " bases")
    plot.runLocal()
  }
}
