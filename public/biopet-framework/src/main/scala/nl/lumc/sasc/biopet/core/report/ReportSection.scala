package nl.lumc.sasc.biopet.core.report

/**
 * Created by pjvan_thof on 4/8/15.
 */
case class ReportSection(location: String,
                         args: Map[String, Any] = Map(),
                         intro: Option[String] = None) {

  def render(args: Map[String, Any]): String = {
    (intro match {
      case Some(template) => ReportBuilder.renderTemplate(location, args ++ this.args)
      case _              => ""
    }) + ReportBuilder.renderTemplate(location, args ++ this.args)
  }
}
