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
package nl.lumc.sasc.biopet.pipelines.mapping

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.report._
import nl.lumc.sasc.biopet.pipelines.bammetrics.BammetricsReport
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport
import nl.lumc.sasc.biopet.pipelines.mapping
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class MappingReport(val parent: Configurable) extends ReportBuilderExtension {
  def builder = MappingReport
}

/**
  * Object ot generate report for [[Mapping]]
  *
  * Created by pjvanthof on 24/06/15.
  */
object MappingReport extends ReportBuilder {

  /** Name of report */
  val reportName = "Mapping Report"

  def pipelineName = "mapping"

  override def extFiles: List[mapping.MappingReport.ExtFile] =
    super.extFiles ++ List("js/gears.js",
                           "js/krona-2.0.js",
                           "img/krona/loading.gif",
                           "img/krona/hidden.png",
                           "img/krona/favicon.ico")
      .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  /** Root page for single BamMetrcis report */
  def indexPage: Future[ReportPage] = {
    val mappingSettings = summary.getSettingKeys(
      runId,
      "mapping",
      NoModule,
      sample = sampleId.map(SampleId).getOrElse(NoSample),
      library = libId.map(LibraryId).getOrElse(NoLibrary),
      keyValues =
        Map("skip_flexiprep" -> List("skip_flexiprep"), "skip_metrics" -> List("skip_metrics"))
    )
    val skipFlexiprep = mappingSettings.get("skip_flexiprep").flatten.getOrElse(false) == true
    val bamMetricsPageValues = BammetricsReport.bamMetricsPageValues(summary,sampleId,libId)
    val flexiprepReportPageValues = FlexiprepReport.flexiprepPageSummaries(summary, sampleId.get, libId.get)
    val bamMetricsPage =
      if (mappingSettings.get("skip_metrics").flatten.getOrElse(false) == false) {
        Some(BammetricsReport.bamMetricsPage(bamMetricsPageValues))
      } else None
  Future {
  ReportPage(
      (if (skipFlexiprep) Nil
       else List("QC" -> FlexiprepReport.flexiprepPage(flexiprepReportPageValues))) :::
        bamMetricsPage.map(_.subPages).getOrElse(Nil),
      List(
        "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/mappingFront.ssp")
      ) ::: bamMetricsPage.map(_.sections).getOrElse(Nil),
      Map()
    )
  }
}
}
