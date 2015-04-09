package nl.lumc.sasc.biopet.core.report

/**
 * Created by pjvan_thof on 3/30/15.
 */
trait MultisampleReportBuilder extends ReportBuilder {
  def samplePage(sampleId: String, args: Map[String, Any]): ReportPage

  def samplesSections: List[(String, ReportSection)] = {
    List(
      ("Samples", ReportSection("/nl/lumc/sasc/biopet/core/report/samplesList.ssp"))
    )
  }

  def libraryPage(libraryId: String, args: Map[String, Any]): ReportPage

  def libririesSections: List[(String, ReportSection)] = {
    List(
      ("Libraries", ReportSection("/nl/lumc/sasc/biopet/core/report/librariesList.ssp"))
    )
  }

  def generateSamplesPage(args: Map[String, Any]): ReportPage = {
    val samplePages = summary.samples
      .map(sampleId => (sampleId -> samplePage(sampleId, args ++ Map("sampleId" -> Some(sampleId)))))
      .toMap
    ReportPage(samplePages, samplesSections, args)
  }

  def generateLibraryPage(args: Map[String, Any]): ReportPage = {
    val libPages = summary.libraries(args("sampleId") match {
      case Some(x) => x.toString
      case None    => throw new IllegalStateException("Sample not found")
    })
      .map(libId => (libId -> libraryPage(libId, args ++ Map("libId" -> Some(libId)))))
      .toMap
    ReportPage(libPages, libririesSections, args)
  }
}
