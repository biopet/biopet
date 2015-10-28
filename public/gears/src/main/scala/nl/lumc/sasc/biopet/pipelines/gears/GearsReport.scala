package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.report._
import nl.lumc.sasc.biopet.utils.config.Configurable

class GearsReport(val root: Configurable) extends ReportBuilderExtension {
  val builder = GearsReport
}

object GearsReport extends ReportBuilder {

  // FIXME: Not yet finished
  // TODO: Sample specific: Add summary (sunflare plot)
  // TODO: Add dustbin analysis (aggregated)
  // TODO: Add alignment stats per sample for the dustbin analysis

  def indexPage = {
    ReportPage(
      List() ++
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

  def reportName = "Gears :: Metagenomics Report"

}
