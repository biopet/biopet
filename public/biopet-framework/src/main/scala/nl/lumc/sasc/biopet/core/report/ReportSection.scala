package nl.lumc.sasc.biopet.core.report

/**
 * Created by pjvan_thof on 4/8/15.
 */
case class ReportSection(location: String,
                         args: Map[String, Any] = Map()) {

  def render(args: Map[String, Any]): String = {
    ReportBuilder.renderTemplate(location, args ++ this.args)
  }
}