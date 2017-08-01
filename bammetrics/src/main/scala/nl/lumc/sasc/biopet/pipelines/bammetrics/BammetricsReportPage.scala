package nl.lumc.sasc.biopet.pipelines.bammetrics

import nl.lumc.sasc.biopet.pipelines.bammetrics.BammetricsReport.mappingQualityPlotTables
import nl.lumc.sasc.biopet.utils.summary.db.Schema.{Library, Sample}
import nl.lumc.sasc.biopet.utils.summary.db.{Schema, SummaryDb}
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.io.File

object BammetricsReportPage {
  def alignmentSummaryValues(summary: SummaryDb,
                             runId: Int,
                             allSamples: Seq[Sample],
                             allLibraries: Seq[Library],
                             sampleId: Option[Int] = None,
                             libId: Option[Int] = None,
                             sampleLevel: Boolean = false,
                             showPlot: Boolean = false,
                             showIntro: Boolean = true,
                             showTable: Boolean = true): Map[String, Any] = {

    val statsPaths = Map(
      "All" -> List("flagstats", "All"),
      "Mapped" -> List("flagstats", "Mapped"),
      "Duplicates" -> List("flagstats", "Duplicates"),
      "NotPrimaryAlignment" -> List("flagstats", "NotPrimaryAlignment")
    )
    val alignmentSummaryResults =
      summary.getStatsForLibraries(runId, "bammetrics", "bamstats", sampleId, statsPaths)
    val alignmentSummaryPlotLines: Option[Seq[String]] =
      if (showPlot)
        Some(BammetricsReport.alignmentSummaryPlotLines(summary, sampleId, !sampleLevel))
      else None
    Map(
      "alignmentSummaryResults" -> alignmentSummaryResults,
      "alignmentSummaryPlotLines" -> alignmentSummaryPlotLines,
      "sampleLevel" -> sampleLevel,
      "showPlot" -> showPlot,
      "showIntro" -> showIntro,
      "showTable" -> showTable,
      "sampleId" -> sampleId,
      "libId" -> libId
    )
  }

  def mappingQualityValues(summary: SummaryDb,
                           runId: Int,
                           allSamples: Seq[Sample],
                           allLibraries: Seq[Library],
                           sampleId: Option[Int],
                           libId: Option[Int],
                           fields: List[String] = List("min", "max", "mean", "median", "modal"),
                           sampleLevel: Boolean = false,
                           showPlot: Boolean = false,
                           showIntro: Boolean = true,
                           showTable: Boolean = true): Map[String, Any] = {

    val samples = sampleId match {
      case Some(id) => allSamples.filter(_.id == id).toList
      case _ => allSamples.toList
    }
    val mapQualityPlotTables: Option[Array[Map[String, Array[Any]]]] =
      if (showPlot)
        Some(mappingQualityPlotTables(summary, !sampleLevel, sampleId, libId))
      else None

    val statsPaths = fields.map(x => x -> List("mapping_quality", "general", x)).toMap
    val mappingQualityTableResults: Map[(Int, Int), Map[String, Option[Any]]] =
      summary.getStatsForLibraries(runId, "bammetrics", "bamstats", sampleId, statsPaths)
    Map(
      "mappingQualityPlotTables" -> mapQualityPlotTables,
      "mappingQualityTableResults" -> mappingQualityTableResults,
      "showIntro" -> showIntro,
      "showTable" -> showTable,
      "showPlot" -> showPlot,
      "sampleId" -> sampleId,
      "libId" -> libId
    )
  }

  def clippingValues(summary: SummaryDb,
                     runId: Int,
                     allSamples: Seq[Sample],
                     allLibraries: Seq[Library],
                     sampleId: Option[Int],
                     libId: Option[Int],
                     fields: List[String] = List("min", "max", "mean", "median", "modal"),
                     sampleLevel: Boolean = false,
                     showPlot: Boolean = false,
                     showIntro: Boolean = true,
                     showTable: Boolean = true): Map[String, Any] = {

    val samples = sampleId match {
      case Some(id) => allSamples.filter(_.id == id).toList
      case _ => allSamples.toList
    }
    val clippingPlotTables: Option[Array[Map[String, Array[Any]]]] =
      if (showPlot)
        Some(BammetricsReport.clippingPlotTables(summary, !sampleLevel, sampleId, libId))
      else None

    val statsPaths = fields.map(x => x -> List("clipping", "general", x)).toMap
    val clippingTableResults =
      summary.getStatsForLibraries(runId, "bammetrics", "bamstats", sampleId, statsPaths)
    Map(
      "clippingPlotTables" -> clippingPlotTables,
      "clippingTableResults" -> clippingTableResults,
      "showIntro" -> showIntro,
      "showTable" -> showTable,
      "showPlot" -> showPlot,
      "sampleId" -> sampleId,
      "libId" -> libId
    )
  }

  def insertSizeValues(summary: SummaryDb,
                       runId: Int,
                       allSamples: Seq[Sample],
                       allLibraries: Seq[Library],
                       sampleId: Option[Int],
                       libId: Option[Int],
                       fields: List[String] =
                         List("mean_insert_size", "standard_deviation", "median_insert_size"),
                       sampleLevel: Boolean = false,
                       showPlot: Boolean = false,
                       showIntro: Boolean = true,
                       showTable: Boolean = true): Map[String, Any] = {

    val samples = sampleId match {
      case Some(id) => allSamples.filter(_.id == id).toList
      case _ => allSamples.toList
    }
    val insertSizePlotTables: Option[Array[Map[String, Array[Any]]]] =
      if (showPlot)
        Some(BammetricsReport.clippingPlotTables(summary, !sampleLevel, sampleId, libId))
      else None

    val statsPaths = fields.map(x => x -> List("metrics", x.toUpperCase)).toMap
    val insertSizeTableResults = summary.getStatsForLibraries(runId,
                                                              "bammetrics",
                                                              "CollectInsertSizeMetrics",
                                                              sampleId,
                                                              statsPaths)
    Map(
      "insertSizePlotTables" -> insertSizePlotTables,
      "insertSizeTableResults" -> insertSizeTableResults,
      "showIntro" -> showIntro,
      "showTable" -> showTable,
      "showPlot" -> showPlot,
      "sampleId" -> sampleId,
      "libId" -> libId
    )
  }
  def rnaHistogramValues(summary: SummaryDb,
                         runId: Int,
                         allSamples: Seq[Sample],
                         allLibraries: Seq[Library],
                         sampleId: Option[Int],
                         libId: Option[Int],
                         fields: List[String] = List("PF_ALIGNED_BASES",
                                                     "MEDIAN_5PRIME_BIAS",
                                                     "MEDIAN_3PRIME_BIAS",
                                                     "MEDIAN_5PRIME_TO_3PRIME_BIAS"),
                         sampleLevel: Boolean = false,
                         showPlot: Boolean = false,
                         showIntro: Boolean = true,
                         showTable: Boolean = true): Map[String, Any] = {

    val samples = sampleId match {
      case Some(id) => allSamples.filter(_.id == id).toList
      case _ => allSamples.toList
    }
    val rnaHistogramPlotTables: Option[Array[Map[String, Array[Any]]]] =
      if (showPlot)
        Some(BammetricsReport.rnaHistogramPlotTables(summary, !sampleLevel, sampleId, libId))
      else None

    val statsPaths = fields.map(x => x -> List("metrics", x.toUpperCase)).toMap
    val rnaHistogramTableResults =
      summary.getStatsForLibraries(runId, "bammetrics", "rna", sampleId, statsPaths)
    Map(
      "rnaHistogramPlotTables" -> rnaHistogramPlotTables,
      "rnaHistogramTableResults" -> rnaHistogramTableResults,
      "showIntro" -> showIntro,
      "showTable" -> showTable,
      "showPlot" -> showPlot,
      "sampleId" -> sampleId,
      "libId" -> libId
    )
  }
  def wgsHistogramValues(summary: SummaryDb,
                         runId: Int,
                         allSamples: Seq[Sample],
                         allLibraries: Seq[Library],
                         sampleId: Option[Int],
                         libId: Option[Int],
                         fields: List[String] = List("mean_coverage",
                                                     "pct_5x",
                                                     "pct_10x",
                                                     "pct_15x",
                                                     "pct_20x",
                                                     "pct_25x",
                                                     "pct_30x",
                                                     "pct_40x",
                                                     "pct_50x",
                                                     "pct_60x",
                                                     "pct_70x",
                                                     "pct_80x",
                                                     "pct_90x",
                                                     "pct_100x"),
                         sampleLevel: Boolean = false,
                         showPlot: Boolean = false,
                         showIntro: Boolean = true,
                         showTable: Boolean = true): Map[String, Any] = {

    val samples = sampleId match {
      case Some(id) => allSamples.filter(_.id == id).toList
      case _ => allSamples.toList
    }
    val wgsHistogramPlotTables: Option[Array[Map[String, Array[Any]]]] =
      if (showPlot)
        Some(BammetricsReport.wgsHistogramPlotTables(summary, !sampleLevel, sampleId, libId))
      else None

    val statsPaths = fields.map(x => x -> List("metrics", x.toUpperCase)).toMap
    val wgsHistogramTableResults =
      summary.getStatsForLibraries(runId, "bammetrics", "wgs", sampleId, statsPaths)
    Map(
      "wgsHistogramPlotTables" -> wgsHistogramPlotTables,
      "wgsHistogramTableResults" -> wgsHistogramTableResults,
      "showIntro" -> showIntro,
      "showTable" -> showTable,
      "showPlot" -> showPlot,
      "sampleId" -> sampleId,
      "libId" -> libId
    )
  }
  def covstatsPlotValues(summary: SummaryDb,
                         runId: Int,
                         sampleId: Option[Int],
                         libId: Option[Int],
                         target: Option[String],
                         metricsTag: String = "bammetrics",
                         fields: List[String] = List("mean",
                                                     "median",
                                                     "max",
                                                     "horizontal",
                                                     "frac_min_10x",
                                                     "frac_min_20x",
                                                     "frac_min_30x",
                                                     "frac_min_40x",
                                                     "frac_min_50x")): Map[String, Any] = {
    val moduleName = target.get + "_cov_stats"
    val plotFile: Option[Schema.File] = Await.result(
      summary.getFile(runId,
                      PipelineName(metricsTag),
                      ModuleName(moduleName),
                      sampleId.map(SampleId).get,
                      libId.map(LibraryId).getOrElse(NoLibrary),
                      "plot"),
      Duration.Inf
    )
    val statsPaths = fields.map(x => x -> List("coverage", "_all", x)).toMap
    val values: Map[String, Option[Any]] = summary.getStatKeys(
      runId,
      PipelineName(metricsTag),
      ModuleName(moduleName),
      sampleId.map(SampleId).get,
      libId.map(LibraryId).getOrElse(NoLibrary),
      statsPaths)
    Map("plotFile" -> plotFile, "values" -> values, "target" -> target, "metricsTag" -> metricsTag)
  }
}
