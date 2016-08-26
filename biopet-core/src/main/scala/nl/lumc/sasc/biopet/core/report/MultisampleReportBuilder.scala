/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.core.report

/**
 * This trait will generate a report with added function to generate sample and library pages for those existing in the summary.
 *
 * @author pjvan_thof
 */
trait MultisampleReportBuilder extends ReportBuilder {

  /** Method to generate a single sample page */
  def samplePage(sampleId: String, args: Map[String, Any]): ReportPage

  /** Default list of samples, can be override */
  def samplesSections: List[(String, ReportSection)] = {
    List(
      ("Samples", ReportSection("/nl/lumc/sasc/biopet/core/report/samplesList.ssp"))
    )
  }

  /** Method to generate a single library page */
  def libraryPage(sampleId: String, libraryId: String, args: Map[String, Any]): ReportPage

  /** Default list of libraries, can be override */
  def librariesSections: List[(String, ReportSection)] = {
    List(
      ("Libraries", ReportSection("/nl/lumc/sasc/biopet/core/report/librariesList.ssp"))
    )
  }

  /** Generate the samples page including a single sample page for each sample in the summary */
  def generateSamplesPage(args: Map[String, Any]): ReportPage = {
    val samplePages = summary.samples
      .map(sampleId => sampleId -> samplePage(sampleId, args ++ Map("sampleId" -> Some(sampleId))))
      .toList
    ReportPage(samplePages, samplesSections, args)
  }

  /** Generate the libraries page for a single sample with a subpage for eacht library */
  def generateLibraryPage(args: Map[String, Any]): ReportPage = {
    val sampleId = args("sampleId") match {
      case Some(x) => x.toString
      case None    => throw new IllegalStateException("Sample not found")
    }

    val libPages = summary.libraries(sampleId)
      .map(libId => libId -> libraryPage(sampleId, libId, args ++ Map("libId" -> Some(libId))))
      .toList
    ReportPage(libPages, librariesSections, args)
  }
}
