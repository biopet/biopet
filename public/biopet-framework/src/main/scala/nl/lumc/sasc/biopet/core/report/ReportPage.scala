package nl.lumc.sasc.biopet.core.report

/**
 * Created by pjvan_thof on 3/27/15.
 */
case class ReportPage(subPages: Map[String, ReportPage],
                      sections: List[(String, ReportSection)],
                      args: Map[String, Any])
