package nl.lumc.sasc.biopet.core.report

/**
 * Created by pjvan_thof on 3/27/15.
 *
 * @param subPages Subpages for this page
 * @param sections Sections for this page
 * @param args Arguments for this page, this arguments get passed to all section and subpages
 */
case class ReportPage(subPages: List[(String, ReportPage)],
                      sections: List[(String, ReportSection)],
                      args: Map[String, Any])
