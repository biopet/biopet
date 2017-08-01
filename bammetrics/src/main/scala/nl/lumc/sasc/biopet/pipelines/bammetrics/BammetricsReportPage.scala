package nl.lumc.sasc.biopet.pipelines.bammetrics

import nl.lumc.sasc.biopet.pipelines.bammetrics.BammetricsReport.mappingQualityPlotTables
import nl.lumc.sasc.biopet.utils.summary.db.Schema.{Library, Sample}
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb._

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
        Some(BammetricsReport.clippingPlotTables(summary,!sampleLevel, sampleId, libId))
      else None

    val statsPaths = fields.map(x => x -> List("clipping", "general", x)).toMap
    val clippingTableResults = summary.getStatsForLibraries(runId, "bammetrics", "bamstats",sampleId, statsPaths)
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
}
