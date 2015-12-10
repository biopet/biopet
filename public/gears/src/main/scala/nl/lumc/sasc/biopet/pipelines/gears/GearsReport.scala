package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.report.{ ReportSection, ReportPage, MultisampleReportBuilder, ReportBuilderExtension }
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Report for Gears
 *
 * Created by pjvan_thof on 12/10/15.
 */
class GearsReport(val root: Configurable) extends ReportBuilderExtension {
  def builder = GearsReport
}

object GearsReport extends MultisampleReportBuilder {

  def reportName = "Gears Report"

  override def extFiles = super.extFiles ++ List("js/gears.js")
    .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  def indexPage = {
    ReportPage(
      List("Samples" -> generateSamplesPage(pageArgs)) ++
        Map(
          "Versions" -> ReportPage(List(), List(
            "Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp")
          ), Map())
        ),
      List(
        "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/gearsFront.ssp")) ++
        List(
          "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp",
            Map("showPlot" -> true, "showTable" -> false)),
          "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp",
            Map("showPlot" -> true, "showTable" -> false))
        ),
      pageArgs
    )
  }

  /** Single sample page */
  def samplePage(sampleId: String, args: Map[String, Any]): ReportPage = {
    ReportPage(List(
      "Libraries" -> generateLibraryPage(args)
    ), List("QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
    ), args)
  }

  /** Library page */
  def libraryPage(sampleId: String, libId: String, args: Map[String, Any]): ReportPage = {
    val krakenExecuted = summary.getValue(Some(sampleId), Some(libId), "gearskraken", "stats", "krakenreport").isDefined

    ReportPage(
      if (krakenExecuted) List("Gears - Metagenomics" -> ReportPage(List(), List(
        "Sunburst analysis" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/gearsSunburst.ssp"
        )), Map()))
      else Nil, List(
        "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
        "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
      ), args)
  }

}