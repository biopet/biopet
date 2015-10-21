package nl.lumc.sasc.biopet.pipelines.gears

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.core.report.MultisampleReportBuilder
import nl.lumc.sasc.biopet.core.report.ReportBuilderExtension
import nl.lumc.sasc.biopet.core.report.ReportPage
import nl.lumc.sasc.biopet.core.report.ReportSection
import nl.lumc.sasc.biopet.utils.config.Configurable

class GearsReport(val root: Configurable) extends ReportBuilderExtension {
  val builder = GearsReport

  override val defaultCoreMemory = 3.0
}

object GearsReport extends MultisampleReportBuilder {

  // FIXME: Not yet finished
  // TODO: Summary all: Add summary (sunflare plot)
  // TODO: Sample specific: Add summary (sunflare plot)
  // TODO: Add dusbin analysis (aggregated)
  // TODO: Add alignment stats per sample for the dustbin analysis

  def indexPage = {
    ReportPage(
      List("Samples" -> generateSamplesPage(pageArgs)) ++
        Map(
          "Versions" -> ReportPage(List(), List((
            "Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp"
            ))), Map())
        ),
      List(
        "Gears intro" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/gearsFront.ssp"),
        "Sunburst analysis" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/gearsSunburst.ssp")
      ),
      pageArgs
    )
  }

  def samplePage(sampleId: String, args: Map[String, Any]) = {
    ReportPage(List(), List(
      "Sunburst analysis" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/gearsSunburst.ssp")
    ), args)
  }

  def libraryPage(sampleId: String, libId: String, args: Map[String, Any]) = {
    ReportPage(List(), List(), args)
  }

  def reportName = "Gears :: Metagenomics Report"

}
