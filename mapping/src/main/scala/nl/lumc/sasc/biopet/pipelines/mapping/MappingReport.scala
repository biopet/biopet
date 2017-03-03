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

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._
import Scalaz._

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

  override def extFiles = super.extFiles ++ List("js/gears.js", "js/krona-2.0.js", "img/krona/loading.gif", "img/krona/hidden.png", "img/krona/favicon.ico")
    .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  def krakenExecuted: Boolean = Await.result(summary.getStatsSize(runId, Some("gears".right), Some(Some("krakenreport".right)),
    sample = Some(sampleId.map(_.left)), library = Some(libId.map(_.left))), Duration.Inf) >= 1

  /** Root page for single BamMetrcis report */
  def indexPage = {
    val mappingSettings = summary.getSettingKeys(runId, "mapping".right, None, sample = sampleId.map(_.left), library = libId.map(_.left),
      keyValues = Map("skip_flexiprep" -> List("skip_flexiprep"), "skip_metrics" -> List("skip_metrics")))
    val skipFlexiprep = mappingSettings.get("skip_flexiprep").flatten.getOrElse(false) == true
    val bamMetricsPage = if (mappingSettings.get("skip_metrics").flatten.getOrElse(false) == true) {
      Some(BammetricsReport.bamMetricsPage(summary, sampleId, libId))
    } else None
    ReportPage((if (skipFlexiprep) Nil else List("QC" -> FlexiprepReport.flexiprepPage)) :::
      bamMetricsPage.map(_.subPages).getOrElse(Nil) ::: List(
        "Versions" -> ReportPage(List(), List("Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp"
        )), Map()),
        "Files" -> ReportPage(List(), Nil, Map())
      ) :::
        (if (krakenExecuted) List("Gears - Metagenomics" -> ReportPage(List(), List(
          "Sunburst analysis" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/gearsSunburst.ssp"
          )), Map()))
        else Nil), List(
      "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/mappingFront.ssp")
    ) ::: bamMetricsPage.map(_.sections).getOrElse(Nil),
      Map()
    )
  }
}
