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
package nl.lumc.sasc.biopet.pipelines.gears

import java.io.File

import nl.lumc.sasc.biopet.core.report.{MultisampleReportBuilder, ReportBuilderExtension, ReportPage, ReportSection}
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport
import nl.lumc.sasc.biopet.pipelines.gears
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.{NoLibrary, NoModule, SampleId}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
 * Report for Gears
 *
 * Created by pjvan_thof on 12/10/15.
 */
class GearsReport(val parent: Configurable) extends ReportBuilderExtension {
  def builder = GearsReport
}

object GearsReport extends MultisampleReportBuilder {

  def pipelineName = "gears"

  def reportName = "Gears Report"

  override def extFiles: List[gears.GearsReport.ExtFile] = super.extFiles ++ List("js/gears.js", "js/krona-2.0.js", "img/krona/loading.gif", "img/krona/hidden.png", "img/krona/favicon.ico")
    .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  def indexPage: Future[ReportPage] = Future {
    val run = Await.result(summary.getRuns(runId).map(_.head), Duration.Inf)

    val krakenExecuted = summary.getStatsSize(runId = runId, pipeline = "gearskraken", module = "krakenreport", library = NoLibrary, mustHaveSample = true) >= samples.size
    val centrifugeExecuted = summary.getStatsSize(runId, "gearscentrifuge", "centrifuge_report", library = NoLibrary, mustHaveSample = true) >= samples.size
    val qiimeClosesOtuTable = summary.getFile(runId, "gears", key = "qiime_closed_otu_table")
    val qiimeOpenOtuTable = summary.getFile(runId, "gears", key = "qiime_open_otu_table")

    val centrifugePage = if (centrifugeExecuted) Some("Centrifuge analysis" -> Future.successful(ReportPage(List("Non-unique" ->
      Future.successful(ReportPage(List(), List("All mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
        Map("summaryStatsTag" -> "centrifuge_report")
      )), Map()))), List(
      "Unique mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
        Map("summaryStatsTag" -> "centrifuge_unique_report")
      )), Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge")))))
    else None

    val krakenPage = if (krakenExecuted) Some("Kraken analysis" -> Future.successful(ReportPage(List(), List(
      "Krona plot" -> Future.successful(ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
      ))), Map())))
    else None

    val qiimeClosedPage = if (qiimeClosesOtuTable.isDefined) Some("Qiime closed reference analysis" -> Future.successful(ReportPage(List(), List(
      "Krona plot" -> Future.successful(ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
      ))), Map("biomFile" -> new File(run.outputDir + File.separator + qiimeClosesOtuTable.get.path)))))
    else None

    val qiimeOpenPage = if (qiimeOpenOtuTable.isDefined) Some("Qiime open reference analysis" -> Future.successful(ReportPage(List(), List(
      "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
      )), Map("biomFile" -> new File(run.outputDir + File.separator + qiimeOpenOtuTable.get.path)))))
    else None

    ReportPage(
      List(centrifugePage, krakenPage, qiimeClosedPage, qiimeOpenPage).flatten ::: List(
        "Samples" -> generateSamplesPage(pageArgs)
      ),
      List(
        "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/gearsFront.ssp")) ++
        List(
          "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp",
            Map("showPlot" -> true, "showTable" -> false)),
          "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp",
            Map("showPlot" -> true, "showTable" -> false))
        ),
      pageArgs
    )
  }

  /** Single sample page */
  def samplePage(sampleId: Int, args: Map[String, Any]): Future[ReportPage] = Future {
    val run = Await.result(summary.getRuns(runId).map(_.head), Duration.Inf)
    val krakenExecuted = Await.result(summary.getStatsSize(runId, "gearskraken", "krakenreport", sample = sampleId, library = NoLibrary), Duration.Inf) == 1
    val centrifugeExecuted = Await.result(summary.getStatsSize(runId, "gearscentrifuge", "centrifuge_report", sample = sampleId, library = None), Duration.Inf) == 1
    val qiimeClosesOtuTable = Await.result(summary.getFile(runId, "gearssingle", NoModule, sampleId, NoLibrary, "qiime_closed_otu_table"), Duration.Inf)
    val qiimeOpenOtuTable = Await.result(summary.getFile(runId, "gearssingle", NoModule, sampleId, NoLibrary, "qiime_open_otu_table"), Duration.Inf)

    ReportPage((if (centrifugeExecuted) List("Centrifuge analysis" -> Future.successful(ReportPage(List(
      "Non-unique" -> Future.successful(ReportPage(List(), List("All mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
        Map("summaryStatsTag" -> "centrifuge_report")
      )), Map()))), List(
      "Unique mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
        Map("summaryStatsTag" -> "centrifuge_unique_report")
      )), Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge")))))
    else Nil) ::: (if (krakenExecuted) List("Kraken analysis" -> Future.successful(ReportPage(List(), List(
      "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
      )), Map())))
    else Nil) ::: (if (qiimeClosesOtuTable.isDefined) List("Qiime closed reference analysis" -> Future.successful(ReportPage(List(), List(
      "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
      )), Map("biomFile" -> new File(run.outputDir + File.separator + qiimeClosesOtuTable.get.path)))))
    else Nil) ::: (if (qiimeOpenOtuTable.isDefined) List("Qiime open reference analysis" -> Future.successful(ReportPage(List(), List(
      "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
      )), Map("biomFile" -> new File(run.outputDir + File.separator + qiimeOpenOtuTable.get.path)))))
    else Nil) ::: List(
      "Libraries" -> generateLibraryPage(args)
    ), List("QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
    ), args)
  }

  /** Library page */
  def libraryPage(sampleId: Int, libId: Int, args: Map[String, Any]): Future[ReportPage] = Future {

    val flexiprepExecuted = Await.result(summary.getStatsSize(runId, "flexiprep", sample = sampleId, library = libId), Duration.Inf) >= 1

    val krakenExecuted = Await.result(summary.getStatsSize(runId, "gearskraken", "krakenreport", sample = sampleId, library = libId), Duration.Inf) == 1
    val centrifugeExecuted = Await.result(summary.getStatsSize(runId, "gearscentrifuge", "centrifuge_report", sample = sampleId, library = libId), Duration.Inf) == 1
    val qiimeClosesOtuTable = Await.result(summary.getFile(runId, "gears", NoModule, sampleId, libId, "qiime_closed_otu_table"), Duration.Inf)
    val qiimeOpenOtuTable = Await.result(summary.getFile(runId, "gears", NoModule, sampleId, libId, "qiime_open_otu_table"), Duration.Inf)

    ReportPage(
      (if (flexiprepExecuted) List("QC" -> FlexiprepReport.flexiprepPage) else Nil
      ) ::: (if (centrifugeExecuted) List("Centrifuge analysis" -> Future.successful(ReportPage(List(
        "Non-unique" -> Future.successful(ReportPage(List(), List("All mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
          Map("summaryStatsTag" -> "centrifuge_report")
        )), Map()))), List(
        "Unique mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
          Map("summaryStatsTag" -> "centrifuge_unique_report")
        )), Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge")))))
      else Nil) ::: (if (krakenExecuted) List("Kraken analysis" -> Future.successful(ReportPage(List(), List(
        "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
        )), Map())))
      else Nil) ::: (if (qiimeClosesOtuTable.isDefined) List("Qiime closed reference analysis" -> Future.successful(ReportPage(List(), List(
        "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
        )), Map("biomFile" -> new File(qiimeClosesOtuTable.get.path)))))
      else Nil) ::: (if (qiimeOpenOtuTable.isDefined) List("Qiime open reference analysis" -> Future.successful(ReportPage(List(), List(
        "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
        )), Map("biomFile" -> new File(qiimeOpenOtuTable.get.path)))))
      else Nil), List(
        "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
        "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
      ), args)
  }

}