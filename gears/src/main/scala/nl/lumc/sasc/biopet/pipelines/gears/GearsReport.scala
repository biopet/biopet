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

import nl.lumc.sasc.biopet.core.report.{
  MultisampleReportBuilder,
  ReportBuilderExtension,
  ReportPage,
  ReportSection
}
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport
import nl.lumc.sasc.biopet.pipelines.gears
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.{ModuleName, NoLibrary, NoModule, SampleId}
import nl.lumc.sasc.biopet.utils.summary.db.Schema.Library
import nl.lumc.sasc.biopet.utils.summary.db.Schema.Sample

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

  override def extFiles: List[gears.GearsReport.ExtFile] =
    super.extFiles ++ List("js/gears.js",
                           "js/krona-2.0.js",
                           "img/krona/loading.gif",
                           "img/krona/hidden.png",
                           "img/krona/favicon.ico")
      .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  def indexPage: Future[ReportPage] = {
    val run = Await.result(summary.getRuns(runId).map(_.head), Duration.Inf)

    val krakenExecuted = summary.getStatsSize(runId = runId,
                                              pipeline = "gearskraken",
                                              module = "krakenreport",
                                              library = NoLibrary,
                                              mustHaveSample = true) >= samples.size
    val centrifugeExecuted = summary.getStatsSize(runId,
                                                  "gearscentrifuge",
                                                  "centrifuge_report",
                                                  library = NoLibrary,
                                                  mustHaveSample = true) >= samples.size
    val qiimeClosesOtuTable = summary.getFile(runId, "gears", key = "qiime_closed_otu_table")
    val qiimeOpenOtuTable = summary.getFile(runId, "gears", key = "qiime_open_otu_table")

    val centrifugePage =
      if (centrifugeExecuted)
        Some(
          "Centrifuge analysis" -> Future.successful(ReportPage(
            List(
              "Non-unique" ->
                Future.successful(
                  ReportPage(List(),
                             List("All mappings" -> ReportSection(
                               "/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
                               Map("summaryStatsTag" -> "centrifuge_report"))),
                             Map()))),
            List("Unique mappings" -> ReportSection(
              "/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
              Map("summaryStatsTag" -> "centrifuge_unique_report"))),
            Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge"))
          )))
      else None

    val krakenPage =
      if (krakenExecuted)
        Some(
          "Kraken analysis" -> Future.successful(
            ReportPage(List(),
                       List("Krona plot" -> Future.successful(
                         ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"))),
                       Map())))
      else None

    val qiimeClosedPage =
      if (qiimeClosesOtuTable.isDefined)
        Some(
          "Qiime closed reference analysis" -> Future.successful(ReportPage(
            List(),
            List("Krona plot" -> Future.successful(
              ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"))),
            Map("biomFile" -> new File(
              run.outputDir + File.separator + qiimeClosesOtuTable.get.path))
          )))
      else None

    val qiimeOpenPage =
      if (qiimeOpenOtuTable.isDefined)
        Some(
          "Qiime open reference analysis" -> Future.successful(ReportPage(
            List(),
            List("Krona plot" -> ReportSection(
              "/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp")),
            Map(
              "biomFile" -> new File(run.outputDir + File.separator + qiimeOpenOtuTable.get.path))
          )))
      else None
    Future {
      ReportPage(
        List(centrifugePage, krakenPage, qiimeClosedPage, qiimeOpenPage).flatten ::: List(
          "Samples" -> generateSamplesPage(pageArgs)
        ),
        List("Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/gearsFront.ssp")) ++
          List(
            "QC reads" -> ReportSection(
              "/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp",
              Map("showPlot" -> true, "showTable" -> false)),
            "QC bases" -> ReportSection(
              "/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp",
              Map("showPlot" -> true, "showTable" -> false))
          ),
        pageArgs
      )
    }
  }

  /** Single sample page */
  def samplePage(sampleId: Int, args: Map[String, Any]): Future[ReportPage] = {
    val run = Await.result(summary.getRuns(runId).map(_.head), Duration.Inf)
    val krakenExecuted = Await.result(summary.getStatsSize(runId,
                                                           "gearskraken",
                                                           "krakenreport",
                                                           sample = sampleId,
                                                           library = NoLibrary),
                                      Duration.Inf) == 1
    val centrifugeExecuted = Await.result(summary.getStatsSize(runId,
                                                               "gearscentrifuge",
                                                               "centrifuge_report",
                                                               sample = sampleId,
                                                               library = None),
                                          Duration.Inf) == 1
    val qiimeClosesOtuTable = Await.result(summary.getFile(runId,
                                                           "gearssingle",
                                                           NoModule,
                                                           sampleId,
                                                           NoLibrary,
                                                           "qiime_closed_otu_table"),
                                           Duration.Inf)
    val qiimeOpenOtuTable = Await.result(
      summary.getFile(runId, "gearssingle", NoModule, sampleId, NoLibrary, "qiime_open_otu_table"),
      Duration.Inf)

    val gearsCentrifugePage = if (centrifugeExecuted) {
      Some(
        "Centrifuge analysis" -> Future.successful(ReportPage(
          List(
            "Non-unique" -> Future.successful(
              ReportPage(List(),
                         List("All mappings" -> ReportSection(
                           "/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
                           Map("summaryStatsTag" -> "centrifuge_report"))),
                         Map()))),
          List("Unique mappings" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
            Map("summaryStatsTag" -> "centrifuge_unique_report"))),
          Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge"))
        )))
    } else None

    val krakenAnalysisPage =
      if (krakenExecuted)
        Some(
          "Kraken analysis" -> Future.successful(
            ReportPage(List(),
                       List("Krona plot" -> ReportSection(
                         "/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp")),
                       Map())))
      else None

    val qiimeClosesOtuTablePage =
      if (qiimeClosesOtuTable.isDefined)
        Some(
          "Qiime closed reference analysis" -> Future.successful(ReportPage(
            List(),
            List("Krona plot" -> ReportSection(
              "/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp")),
            Map("biomFile" -> new File(
              run.outputDir + File.separator + qiimeClosesOtuTable.get.path))
          )))
      else None

    val qiimeOpenOtuTablePage =
      if (qiimeOpenOtuTable.isDefined)
        Some(
          "Qiime open reference analysis" -> Future.successful(ReportPage(
            List(),
            List("Krona plot" -> ReportSection(
              "/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp")),
            Map(
              "biomFile" -> new File(run.outputDir + File.separator + qiimeOpenOtuTable.get.path))
          )))
      else None

    Future {
      ReportPage(
        subPages =
          List(gearsCentrifugePage.toList,
               krakenAnalysisPage,
               qiimeClosesOtuTablePage,
               qiimeOpenOtuTablePage).flatten ::: List(
            "Libraries" -> generateLibraryPage(args)
          ),
        sections = List(
          "QC reads" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
          "QC bases" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
        ),
        args = args
      )
    }
  }

  /** Library page */
  def libraryPage(sampleId: Int, libId: Int, args: Map[String, Any]): Future[ReportPage] = {
    val sName = Await.result(summary.getSampleName(sampleId), Duration.Inf)
    val lName = Await.result(summary.getLibraryName(libId), Duration.Inf)

    val flexiprepExecuted = Await.result(
      summary.getStatsSize(runId, "flexiprep", sample = sampleId, library = libId),
      Duration.Inf) >= 1

    val krakenExecuted = Await.result(summary.getStatsSize(runId,
                                                           "gearskraken",
                                                           "krakenreport",
                                                           sample = sampleId,
                                                           library = libId),
                                      Duration.Inf) == 1
    val centrifugeExecuted = Await.result(summary.getStatsSize(runId,
                                                               "gearscentrifuge",
                                                               "centrifuge_report",
                                                               sample = sampleId,
                                                               library = libId),
                                          Duration.Inf) == 1
    val qiimeClosesOtuTable = Await.result(
      summary.getFile(runId, "gears", NoModule, sampleId, libId, "qiime_closed_otu_table"),
      Duration.Inf)
    val qiimeOpenOtuTable = Await.result(
      summary.getFile(runId, "gears", NoModule, sampleId, libId, "qiime_open_otu_table"),
      Duration.Inf)

    val flexiprepReportPage =
      if (flexiprepExecuted) Some("QC" -> FlexiprepReport.flexiprepPage) else None

    val centrifugeReportPage =
      if (centrifugeExecuted)
        Some(
          "Centrifuge analysis" -> Future.successful(ReportPage(
            List(
              "Non-unique" -> Future.successful(
                ReportPage(List(),
                           List("All mappings" -> ReportSection(
                             "/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
                             Map("summaryStatsTag" -> "centrifuge_report"))),
                           Map()))),
            List("Unique mappings" -> ReportSection(
              "/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
              Map("summaryStatsTag" -> "centrifuge_unique_report"))),
            Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge"))
          )))
      else None

    val krakenAnalysisPage =
      if (krakenExecuted)
        List(
          "Kraken analysis" -> Future.successful(
            ReportPage(List(),
                       List("Krona plot" -> ReportSection(
                         "/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp")),
                       Map())))
      else Nil

    val qiimeClosesOtuTablePage =
      if (qiimeClosesOtuTable.isDefined)
        Some(
          "Qiime closed reference analysis" -> Future.successful(ReportPage(
            List(),
            List("Krona plot" -> ReportSection(
              "/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp")),
            Map("biomFile" -> new File(qiimeClosesOtuTable.get.path))
          )))
      else None

    val qiimeOpenOtuTablePage =
      if (qiimeOpenOtuTable.isDefined)
        Some(
          "Qiime open reference analysis" -> Future.successful(
            ReportPage(List(),
                       List("Krona plot" -> ReportSection(
                         "/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp")),
                       Map("biomFile" -> new File(qiimeOpenOtuTable.get.path)))))
      else None

    Future {
      ReportPage(
        List(flexiprepReportPage,
             centrifugeReportPage,
             krakenAnalysisPage,
             qiimeClosesOtuTablePage,
             qiimeOpenOtuTablePage).flatten,
        List(
          "QC reads" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
          "QC bases" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
        ),
        args
      )
    }
  }

}

object GearsKronaPlot {
  def map(summary: SummaryDb,
          runId: Int,
          sampleId: Option[Int],
          libId: Option[Int],
          summaryModuleTag: String,
          summaryStatsTag: String,
          centrifugeTag: Option[String],
          allSamples: Seq[Sample],
          allLibraries: Seq[Library]): Map[String, Any] = {
    Map(
      "summary" -> summary,
      "runId" -> runId,
      "sampleId" -> sampleId,
      "libId" -> libId,
      "summaryModuleTag" -> summaryModuleTag,
      "summaryStatsTag" -> summaryStatsTag,
      "centrifugeTag" -> centrifugeTag,
      "allSamples" -> allSamples,
      "allLibraries" -> allLibraries,
      "summaries" -> summaries(summary, runId, sampleId, libId, summaryModuleTag, summaryStatsTag),
      "totalReads" -> totalReads(summary,
                                 runId,
                                 sampleId,
                                 libId,
                                 summaryModuleTag,
                                 centrifugeTag,
                                 allSamples,
                                 allLibraries)
    )
  }

  def summaries(summary: SummaryDb,
                runId: Int,
                sampleId: Option[Int],
                libId: Option[Int],
                summaryModuleTag: String,
                summaryStatsTag: String): Map[Int, Map[String, Option[Any]]] = {
    if (libId.isDefined)
      summary
        .getStatsForLibraries(runId,
                              summaryModuleTag,
                              summaryStatsTag,
                              sampleId,
                              Map("all" -> Nil))
        .filter(_._1._2 == libId.get)
        .map(x => x._1._1 -> x._2)
    else
      summary.getStatsForSamples(runId,
                                 summaryModuleTag,
                                 summaryStatsTag,
                                 sampleId.map(SampleId),
                                 Map("all" -> Nil))
  }
  def totalReads(
      summary: SummaryDb,
      runId: Int,
      sampleId: Option[Int],
      libId: Option[Int],
      summaryModuleTag: String,
      centrifugeTag: Option[String],
      allSamples: Seq[Sample],
      allLibraries: Seq[Library]
  ): Option[Map[String, Long]] = {
    centrifugeTag.map { tag =>
      if (libId.isDefined) {
        val stats = summary
          .getStatsForLibraries(runId,
                                summaryModuleTag,
                                ModuleName(tag),
                                sampleId,
                                Map("total" -> List("metrics", "Read")))
          .filter(_._1._2 == libId.get)
          .head
        val lib = allLibraries.filter(_.id == stats._1._2).head
        val sample = allSamples.filter(_.id == stats._1._1).head
        Map(s"${sample.name}" -> stats._2("total").map(_.toString.toLong).getOrElse(0L))
      } else
        summary
          .getStatsForSamples(runId,
                              summaryModuleTag,
                              ModuleName(tag),
                              sampleId.map(SummaryDb.SampleId),
                              Map("total" -> List("metrics", "Read")))
          .map(
            x =>
              allSamples
                .find(_.id == x._1)
                .head
                .name -> x._2("total").map(_.toString.toLong).getOrElse(0L))
    }
  }

}
