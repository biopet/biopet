package nl.lumc.sasc.biopet.pipelines.bammetrics

import nl.lumc.sasc.biopet.core.report.{ ReportBuilder, ReportPage, ReportSection }

/**
 * Created by pjvan_thof on 3/30/15.
 */
object BammetricsReport extends ReportBuilder {
  val reportName = "Bam Metrics"

  def indexPage = ReportPage(Map(
    "Bam Metrics" -> bamMetricsPage
  ), List(
    "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/bamMetricsFront.ssp")
  ),
    Map()
  )

  def bamMetricsPage = ReportPage(
    Map(),
    List(
      "Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp"),
      "Bam Stats" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/bamStats.ssp"),
      "Insert Size" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/insertSize.ssp"),
      "RNA (optional)" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/rna.ssp"),
      "Target (optional)" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/target.ssp"),
      "GC Bias" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/gcBias.ssp")
    ),
    Map()
  )

  // FIXME: Not yet finished
}
