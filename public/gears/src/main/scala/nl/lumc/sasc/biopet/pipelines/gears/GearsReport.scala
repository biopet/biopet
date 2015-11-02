package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.report._
import nl.lumc.sasc.biopet.utils.config.Configurable

class GearsReport(val root: Configurable) extends ReportBuilderExtension {
  val builder = GearsReport
}

object GearsReport extends ReportBuilder {

  // TODO: Add dustbin analysis (aggregated)
  // TODO: Add alignment stats per sample for the dustbin analysis

  override def extFiles = super.extFiles ++ List("js/gears.js")
    .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  def indexPage = {
    ReportPage(
      List(
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
