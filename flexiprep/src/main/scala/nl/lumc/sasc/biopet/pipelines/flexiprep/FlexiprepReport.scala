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

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.core.report.{
  ReportBuilder,
  ReportBuilderExtension,
  ReportPage,
  ReportSection
}
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.rscript.StackedBarPlot
import nl.lumc.sasc.biopet.utils.summary.db.Schema.{Library, Sample}
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb._

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

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
  def indexPage: Future[ReportPage] = this.flexiprepPage(summary, sampleId.get, libId.get).map {
    page =>
      ReportPage(
        Nil,
        List(
          "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepFront.ssp")
        ) ::: page.sections,
        Map())
  }

  /** Generate a QC report page for 1 single library, sampleId and libId must be defined in the arguments */
  def flexiprepPage(summary: SummaryDb, sampleId: Int, libId: Int): Future[ReportPage] = {
    val flexiprepReadSummary = FlexiprepReadSummary.values(summary,
                                                           runId,
                                                           samples,
                                                           libraries,
                                                           sampleId,
                                                           libId,
                                                           multisample = false)
    val flexiprepBaseSummary = FlexiprepBaseSummary.values(summary,
                                                           runId,
                                                           samples,
                                                           libraries,
                                                           sampleId,
                                                           libId,
                                                           multisample = false)
    Future(
      ReportPage(
        List(),
        List(
          "Read Summary" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp",
            flexiprepReadSummary),
          "Base Summary" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp",
            flexiprepBaseSummary),
          fastqcPlotSection("Base quality", "plot_per_base_quality"),
          fastqcPlotSection("Sequence quality", "plot_per_sequence_quality"),
          fastqcPlotSection("Base GC content", "plot_per_base_gc_content"),
          fastqcPlotSection("Sequence GC content", "plot_per_sequence_gc_content"),
          fastqcPlotSection("Base sequence content", "plot_per_base_sequence_content"),
          fastqcPlotSection("Duplication", "plot_duplication_levels"),
          fastqcPlotSection("Kmers", "plot_kmer_profiles"),
          fastqcPlotSection("Length distribution", "plot_sequence_length_distribution"),
          fastqcPlotSection("Adapters", "plot_adapter_content")
        ),
        Map()
      ))
  }

  protected def fastqcPlotSection(name: String, tag: String): (String, ReportSection) = {
    name -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepFastQcPlot.ssp",
                          Map("plot" -> tag))
  }

  /**
    * Generates the lines for readSummaryPlot
    *  @param read Must give "R1" or "R2"
    * @param summary Summary class
    * @param sampleId Default selects all samples, when given plot is limits on given sample
    */
  def readSummaryPlotLines(read: String,
                           summary: SummaryDb,
                           sampleId: Option[Int] = None): Seq[String] = {

    val seqstatPaths = Map("num_total" -> List("reads", "num_total"))
    val seqstatStats =
      summary.getStatsForLibraries(runId, "flexiprep", "seqstat_" + read, keyValues = seqstatPaths)
    val seqstatQcStats = summary.getStatsForLibraries(runId,
                                                      "flexiprep",
                                                      "seqstat_" + read + "_qc",
                                                      keyValues = seqstatPaths)
    val clippingPaths = Map(
      "num_reads_discarded_too_short" -> List("num_reads_discarded_too_short"),
      "num_reads_discarded_too_long" -> List("num_reads_discarded_too_long"))
    val clippingStats = summary.getStatsForLibraries(runId,
                                                     "flexiprep",
                                                     "clipping_" + read,
                                                     keyValues = clippingPaths)

    val trimmingPaths = Map("num_reads_discarded" -> List("num_reads_discarded_" + read))
    val trimmingStats =
      summary.getStatsForLibraries(runId, "flexiprep", "trimming", keyValues = trimmingPaths)

    val libraries =
      Await.result(summary.getLibraries(runId = runId, sampleId = sampleId), Duration.Inf)

    val summaryPlotLines = ArrayBuffer[String]()

    for (lib <- libraries) {
      val beforeTotal =
        seqstatStats((lib.sampleId, lib.id))("num_total").getOrElse(0).toString.toLong
      val afterTotal =
        seqstatQcStats((lib.sampleId, lib.id))("num_total").getOrElse(0).toString.toLong
      val clippingDiscardedToShort = clippingStats((lib.sampleId, lib.id))(
        "num_reads_discarded_too_short").getOrElse(0).toString.toLong
      val clippingDiscardedToLong = clippingStats((lib.sampleId, lib.id))(
        "num_reads_discarded_too_long").getOrElse(0).toString.toLong
      val trimmingDiscarded =
        trimmingStats((lib.sampleId, lib.id))("num_reads_discarded").getOrElse(0).toString.toLong

      val sb = new StringBuffer()
      sb.append(
        Await
          .result(summary.getSampleName(lib.sampleId), Duration.Inf)
          .getOrElse("") + "-" + lib.name + "\t")
      sb.append(afterTotal + "\t")
      sb.append((clippingDiscardedToShort + clippingDiscardedToLong) + "\t")
      sb.append(trimmingDiscarded + "\t")
      sb.append(
        beforeTotal - afterTotal - trimmingDiscarded - clippingDiscardedToShort - clippingDiscardedToLong)
      summaryPlotLines += sb.toString
    }
    summaryPlotLines
  }

  /**
    * Generated a stacked bar plot for reads QC
    * @param outputDir OutputDir for plot
    * @param prefix prefix for tsv and png file
    * @param read Must give "R1" or "R2"
    * @param summaryPlotLines A sequence of strings written to the summary tsv
    */
  def readSummaryPlot(outputDir: File,
                      prefix: String,
                      read: String,
                      summaryPlotLines: Seq[String]): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    tsvWriter.println("Library\tAfter_QC\tClipping\tTrimming\tSynced")

    for (line <- summaryPlotLines)
      tsvWriter.println(line)
    tsvWriter.close()

    val plot = new StackedBarPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("Reads")
    plot.width = Some(200 + (summaryPlotLines.size * 10))
    plot.title = Some("QC summary on " + read + " reads")
    plot.runLocal()
  }

  /**
    * Generates the lines for baseSummaryPlotLines
    * @param read Must give "R1" or "R2"
    * @param summary Summary class
    * @param sampleId Default selects all samples, when given plot is limits on given sample
    */
  def baseSummaryPlotLines(read: String,
                           summary: SummaryDb,
                           sampleId: Option[Int] = None): Seq[String] = {

    val statsPaths = Map("num_total" -> List("bases", "num_total"))
    val seqstatStats =
      summary.getStatsForLibraries(runId, "flexiprep", "seqstat_" + read, keyValues = statsPaths)
    val seqstatQcStats = summary.getStatsForLibraries(runId,
                                                      "flexiprep",
                                                      "seqstat_" + read + "_qc",
                                                      keyValues = statsPaths)

    val libraries =
      Await.result(summary.getLibraries(runId = runId, sampleId = sampleId), Duration.Inf)

    val summaryPlotLines = ArrayBuffer[String]()
    for (lib <- libraries) {
      val beforeTotal =
        seqstatStats((lib.sampleId, lib.id))("num_total").getOrElse(0).toString.toLong
      val afterTotal =
        seqstatQcStats((lib.sampleId, lib.id))("num_total").getOrElse(0).toString.toLong

      val sb = new StringBuffer()
      sb.append(
        Await
          .result(summary.getSampleName(lib.sampleId), Duration.Inf)
          .getOrElse("") + "-" + lib + "\t")
      sb.append(afterTotal + "\t")
      sb.append(beforeTotal - afterTotal)

      summaryPlotLines += sb.toString
    }
    summaryPlotLines
  }

  /**
    * Generated a stacked bar plot for bases QC
    * @param outputDir OutputDir for plot
    * @param prefix prefix for tsv and png file
    * @param read Must give "R1" or "R2"
    * @param summaryPlotLines A sequence of strings written to the summary tsv
    */
  def baseSummaryPlot(outputDir: File,
                      prefix: String,
                      read: String,
                      summaryPlotLines: Seq[String]): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    tsvWriter.println("Library\tAfter_QC\tDiscarded")

    for (line <- summaryPlotLines) {
      tsvWriter.println(line)
    }

    tsvWriter.close()

    val plot = new StackedBarPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("Bases")
    plot.width = Some(200 + (summaryPlotLines.size * 10))
    plot.title = Some("QC summary on " + read + " bases")
    plot.runLocal()
  }
}

object FlexiprepReadSummary {
  def values(summary: SummaryDb,
             runId: Int,
             allSamples: Seq[Sample],
             allLibraries: Seq[Library],
             sampleId: Option[Int] = None,
             libId: Option[Int] = None,
             showPlot: Boolean = false,
             showTable: Boolean = true,
             showIntro: Boolean = true,
             multisample: Boolean = true): Map[String, Any] = {

    val settings = summary.getSettingsForLibraries(runId,
                                                   "flexiprep",
                                                   keyValues =
                                                     Map("skip_trim" -> List("skip_trim"),
                                                         "skip_clip" -> List("skip_clip"),
                                                         "paired" -> List("paired")))
    settings.count(_._2.getOrElse("skip_trim", None).contains(true))
    val paired =
      if (sampleId.isDefined && libId.isDefined)
        summary
          .getSettingKeys(runId,
                          "flexiprep",
                          NoModule,
                          SampleId(sampleId.get),
                          LibraryId(libId.get),
                          keyValues = Map("paired" -> List("paired")))
          .getOrElse("paired", None)
          .contains(true)
      else settings.count(_._2.getOrElse("paired", None).contains(true)) >= 1

    val samples = sampleId.map(id => allSamples.filter(_.id == id)).getOrElse(allSamples)
    val libraries = libId.map(id => allLibraries.filter(_.id == id)).getOrElse(allLibraries)
    val trimCount = settings.count(_._2.getOrElse("skip_trim", None).contains(false))
    val clipCount = settings.count(_._2.getOrElse("skip_clip", None).contains(false))
    val librariesCount = libraries.size

    val summaryPlotLinesR1: Option[Seq[String]] =
      if (showPlot)
        Some(FlexiprepReport.readSummaryPlotLines("R1", summary, sampleId = sampleId))
      else None
    val summaryPlotlinesR2: Option[Seq[String]] =
      if (showPlot && paired)
        Some(FlexiprepReport.readSummaryPlotLines("R2", summary, sampleId = sampleId))
      else None

    val seqstatPaths = Map("num_total" -> List("reads", "num_total"))
    val clippingPaths = Map(
      "num_reads_discarded_too_short" -> List("num_reads_discarded_too_short"),
      "num_reads_discarded_too_long" -> List("num_reads_discarded_too_long"))
    val trimmingPaths = Map("num_reads_discarded" -> List("num_reads_discarded_total"))

    val seqstatStats: Map[(Int, Int), Map[String, Option[Any]]] =
      summary.getStatsForLibraries(runId, "flexiprep", "seqstat_R1", sampleId, seqstatPaths)
    val seqstatQCStats =
      summary.getStatsForLibraries(runId, "flexiprep", "seqstat_R1_QC", sampleId, seqstatPaths)
    val clippingStats =
      summary.getStatsForLibraries(runId, "flexiprep", "seqstat_R1", sampleId, clippingPaths)
    val trimmingStats =
      summary.getStatsForLibraries(runId, "flexiprep", "seqstat_R1", sampleId, trimmingPaths)

    Map(
      "runId" -> runId,
      "sampleId" -> sampleId,
      "libId" -> libId,
      "showPlot" -> showPlot,
      "settings" -> settings,
      "trimCount" -> trimCount,
      "clipCount" -> clipCount,
      "librariesCount" -> librariesCount,
      "seqstatStats" -> seqstatStats,
      "seqstatQCStats" -> seqstatQCStats,
      "clippingStats" -> clippingStats,
      "trimmingStats" -> trimmingStats,
      "summaryPlotLinesR1" -> summaryPlotLinesR1,
      "summaryPlotLinesR2" -> summaryPlotlinesR2,
      "paired" -> paired
    )
  }
}

object FlexiprepBaseSummary {
  def values(summary: SummaryDb,
             runId: Int,
             allSamples: Seq[Sample],
             allLibraries: Seq[Library],
             sampleId: Option[Int] = None,
             libId: Option[Int] = None,
             showPlot: Boolean = false,
             showTable: Boolean = true,
             showIntro: Boolean = true,
             multisample: Boolean = true): Map[String, Any] = {

    val samples = sampleId.map(id => allSamples.filter(_.id == id)).getOrElse(allSamples)
    val libraries = libId.map(id => allLibraries.filter(_.id == id)).getOrElse(allLibraries)
    val librariesCount = libraries.size
    val settings = summary.getSettingsForLibraries(runId,
                                                   "flexiprep",
                                                   keyValues =
                                                     Map("skip_trim" -> List("skip_trim"),
                                                         "skip_clip" -> List("skip_clip"),
                                                         "paired" -> List("paired")))
    settings.count(_._2.getOrElse("skip_trim", None) == Some(true))
    val trimCount = settings.count(_._2.getOrElse("skip_trim", None) == Some(false))
    val clipCount = settings.count(_._2.getOrElse("skip_clip", None) == Some(false))

    val paired: Boolean =
      if (sampleId.isDefined && libId.isDefined)
        summary
          .getSettingKeys(runId,
                          "flexiprep",
                          NoModule,
                          SampleId(sampleId.get),
                          LibraryId(libId.get),
                          keyValues = Map("paired" -> List("paired")))
          .getOrElse("paired", None) == Some(true)
      else settings.count(_._2.getOrElse("paired", None) == Some(true)) >= 1

    val summaryPlotLinesR1: Option[Seq[String]] =
      if (showPlot)
        Some(FlexiprepReport.baseSummaryPlotLines("R1", summary, sampleId = sampleId))
      else None
    val summaryPlotlinesR2: Option[Seq[String]] =
      if (showPlot && paired)
        Some(FlexiprepReport.baseSummaryPlotLines("R2", summary, sampleId = sampleId))
      else None

    val statsPaths = Map("num_total" -> List("bases", "num_total"))

    val seqstatStats =
      summary.getStatsForLibraries(runId, "flexiprep", "seqstat_R1", sampleId, statsPaths)
    val seqstatQCStats =
      summary.getStatsForLibraries(runId, "flexiprep", "seqstat_R1_QC", sampleId, statsPaths)

    Map(
      "runId" -> runId,
      "sampleId" -> sampleId,
      "libId" -> libId,
      "showPlot" -> showPlot,
      "settings" -> settings,
      "trimCount" -> trimCount,
      "clipCount" -> clipCount,
      "librariesCount" -> librariesCount,
      "seqstatStats" -> seqstatStats,
      "seqstatQCStats" -> seqstatQCStats,
      "summaryPlotLinesR1" -> summaryPlotLinesR1,
      "summaryPlotLinesR2" -> summaryPlotlinesR2,
      "paired" -> paired
    )
  }
}
