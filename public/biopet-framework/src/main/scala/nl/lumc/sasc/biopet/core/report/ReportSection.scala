package nl.lumc.sasc.biopet.core.report

/**
 * Created by pjvan_thof on 4/8/15.
 *
 * @param location Location inside the classpath / jar
 * @param args arguments only for current section, this is not passed to other sub pages
 */
case class ReportSection(location: String,
                         args: Map[String, Any] = Map()) {
  /**
   * This method will render this section
   * @param args Possible to give more arguments
   * @return Rendered result for this section
   */
  def render(args: Map[String, Any] = Map()): String = {
    ReportBuilder.renderTemplate(location, args ++ this.args)
  }
}
