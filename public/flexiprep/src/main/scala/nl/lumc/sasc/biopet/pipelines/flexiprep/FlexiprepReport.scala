package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core.report.{ ReportPage, ReportBuilder }

/**
 * Created by pjvan_thof on 3/30/15.
 */
object FlexiprepReport extends ReportBuilder {
  val reportName = "Flexiprep"

  def indexPage = {
    ReportPage(Map(), Map("Summary" -> "/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepSummary.ssp"), Map())
  }

  // FIXME: Not yet finished
}
