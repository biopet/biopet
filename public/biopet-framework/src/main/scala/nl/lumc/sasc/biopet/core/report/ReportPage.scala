package nl.lumc.sasc.biopet.core.report

import java.net.URL

/**
 * Created by pjvan_thof on 3/27/15.
 */
case class ReportPage(val subPages: Map[String, ReportPage],
                      val sections: Map[String, String],
                      val args: Map[String, Any])
