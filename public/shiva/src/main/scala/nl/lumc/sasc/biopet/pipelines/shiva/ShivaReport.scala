package nl.lumc.sasc.biopet.pipelines.shiva

import nl.lumc.sasc.biopet.core.report.{ReportSection, MultisampleReportBuilder, ReportPage}
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport

/**
 * Created by pjvan_thof on 3/30/15.
 */
object ShivaReport extends MultisampleReportBuilder {
  def samplePage(sampleId: String, args: Map[String, Any]) = {
    ReportPage(Map("Libraries" -> generateLibraryPage(args)), List(
      "Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/core/report/ShivaVariantcalling.ssp"),
      "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
    ), args)
  }

  def libraryPage(libId: String, args: Map[String, Any]) = {
    ReportPage(Map("Flexiprep" -> FlexiprepReport.indexPage), List(
      "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
    ), args)
  }

  def reportName = "Title Test"

  def generalPage = ReportPage(Map(), List(
    "Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/core/report/ShivaVariantcalling.ssp"),
    "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
    "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
  ), Map())

  // FIXME: Not yet finished
}
