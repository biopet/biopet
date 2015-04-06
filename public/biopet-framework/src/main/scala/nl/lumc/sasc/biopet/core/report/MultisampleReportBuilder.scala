package nl.lumc.sasc.biopet.core.report

import nl.lumc.sasc.biopet.core.summary.Summary

/**
 * Created by pjvan_thof on 3/30/15.
 */
trait MultisampleReportBuilder extends ReportBuilder {
  def generalPage: ReportPage

  def samplePage(sampleId: String, args: Map[String, Any]): ReportPage

  def libraryPage(libraryId: String, args: Map[String, Any]): ReportPage

  def generateSamplesPage(args: Map[String, Any]): ReportPage = {
    val samplePages = summary.samples
      .map(sampleId => (sampleId -> samplePage(sampleId, args ++ Map("sampleId" -> Some(sampleId)))))
      .toMap
    ReportPage(samplePages, Map(), args)
  }

  def generateLibraryPage(args: Map[String, Any]): ReportPage = {
    val libPages = summary.libraries(args("sampleId") match {
      case Some(x) => x.toString
      case None    => throw new IllegalStateException("Sample not found")
    })
      .map(libId => (libId -> libraryPage(libId, args ++ Map("libId" -> Some(libId)))))
      .toMap
    ReportPage(libPages, Map(), args)
  }

  def indexPage = ReportPage(Map("General" -> generalPage, "Samples" -> generateSamplesPage(pageArgs)), Map(), pageArgs)
}
