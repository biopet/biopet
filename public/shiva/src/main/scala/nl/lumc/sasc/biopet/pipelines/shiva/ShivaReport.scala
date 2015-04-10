package nl.lumc.sasc.biopet.pipelines.shiva

import nl.lumc.sasc.biopet.core.report.{ ReportSection, MultisampleReportBuilder, ReportPage }
import nl.lumc.sasc.biopet.pipelines.bammetrics.BammetricsReport
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport

/**
 * Created by pjvan_thof on 3/30/15.
 */
object ShivaReport extends MultisampleReportBuilder {
  def indexPage = ReportPage(
    Map(
      /*"General" -> ReportPage(Map(), List(
        "Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp"),
        "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp", Map("sampleLevel" -> true)),
        "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
        "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
      ), Map()),*/
      "Samples" -> generateSamplesPage(pageArgs)
    ),
    List(
      "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/shivaFront.ssp"),
      "Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp"),
      "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp", Map("sampleLevel" -> true)),
      "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
    ),
    pageArgs
  )

  def samplePage(sampleId: String, args: Map[String, Any]) = {
    ReportPage(Map(
      "Libraries" -> generateLibraryPage(args),
      "Alignment" -> BammetricsReport.bamMetricsPage
    ), List(
      "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp"),
      "Preprocessing" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp", Map("sampleLevel" -> true)),
      "Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp"),
      "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
    ), args)
  }

  def libraryPage(libId: String, args: Map[String, Any]) = {
    ReportPage(Map(
      "Alignment" -> BammetricsReport.bamMetricsPage,
      "QC" -> FlexiprepReport.flexiprepPage
    ), List(
      "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp"),
      "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
    ), args)
  }

  def reportName = "Shiva Report"

  // FIXME: Not yet finished
}
