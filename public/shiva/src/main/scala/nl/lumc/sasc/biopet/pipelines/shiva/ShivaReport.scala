package nl.lumc.sasc.biopet.pipelines.shiva

import nl.lumc.sasc.biopet.core.report.{MultisampleReportBuilder, ReportPage}
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport

/**
 * Created by pjvan_thof on 3/30/15.
 */
object ShivaReport extends MultisampleReportBuilder {
  def samplePage(sampleId: String, args: Map[String, Any]) = {
    ReportPage(Map("Libraries" -> generateLibraryPage(args)), Map(
      "Variantcalling" -> "/nl/lumc/sasc/biopet/core/report/ShivaVariantcalling.ssp"
    ), args)
  }

  def libraryPage(libId: String, args: Map[String, Any]) = {
    ReportPage(Map("Flexiprep" -> FlexiprepReport.indexPage), Map(), args)
  }

  def reportName = "Title Test"

  def generalPage = ReportPage(Map(), Map(
    "Variantcalling" -> "/nl/lumc/sasc/biopet/core/report/ShivaVariantcalling.ssp",
    "Flexiprep" -> "/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepSummary.ssp"
  ), Map())

  // FIXME: Not yet finished
}
