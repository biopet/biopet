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

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This trait will generate a report with added function to generate sample and library pages for those existing in the summary.
 *
 * @author pjvan_thof
 */
trait MultisampleReportBuilder extends ReportBuilder {

  /** Method to generate a single sample page */
  def samplePage(sampleId: Int, args: Map[String, Any]): Future[ReportPage]

  /** Default list of samples, can be override */
  def samplesSections: List[(String, ReportSection)] = {
    List(
      ("Samples", ReportSection("/nl/lumc/sasc/biopet/core/report/samplesList.ssp"))
    )
  }

  /** Method to generate a single library page */
  def libraryPage(sampleId: Int, libraryId: Int, args: Map[String, Any]): Future[ReportPage]

  /** Default list of libraries, can be override */
  def librariesSections: List[(String, ReportSection)] = {
    List(
      ("Libraries", ReportSection("/nl/lumc/sasc/biopet/core/report/librariesList.ssp"))
    )
  }

  /** Generate the samples page including a single sample page for each sample in the summary */
  def generateSamplesPage(args: Map[String, Any]): Future[ReportPage] = Future {
    val samples = Await.result(summary.getSamples(runId = Some(runId)), Duration.Inf)
    val samplePages = samples.map(_.id)
      .map(sampleId => sampleId -> samplePage(sampleId, args ++ Map("sampleId" -> Some(sampleId))))
      .toList
    ReportPage(samplePages.map(x => samples.find(_.id == x._1).get.name -> x._2), samplesSections, args)
  }

  /** Generate the libraries page for a single sample with a subpage for eacht library */
  def generateLibraryPage(args: Map[String, Any]): Future[ReportPage] = Future {
    val sampleId = args("sampleId") match {
      case Some(x: Int) => x
      case None         => throw new IllegalStateException("Sample not found")
    }

    val libraries = Await.result(summary.getLibraries(runId = Some(runId), sampleId = Some(sampleId)), Duration.Inf)

    val libPages = libraries.map(_.id)
      .map(libId => libId -> libraryPage(sampleId, libId, args ++ Map("libId" -> Some(libId))))
      .toList
    ReportPage(libPages.map(x => libraries.find(_.id == x._1).get.name -> x._2), librariesSections, args)
  }
}
