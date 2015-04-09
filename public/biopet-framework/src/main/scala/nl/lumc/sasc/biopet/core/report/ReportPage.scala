package nl.lumc.sasc.biopet.core.report

/**
 * Created by pjvan_thof on 3/27/15.
 */
case class ReportPage(val subPages: Map[String, ReportPage],
                      val sections: List[(String, ReportSection)],
                      val args: Map[String, Any])
