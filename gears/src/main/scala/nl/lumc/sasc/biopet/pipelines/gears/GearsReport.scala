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

import nl.lumc.sasc.biopet.utils.implicits.Either._
import nl.lumc.sasc.biopet.core.report.{ MultisampleReportBuilder, ReportBuilderExtension, ReportPage, ReportSection }
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.concurrent.Await
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

  def reportName = "Gears Report"

  override def extFiles = super.extFiles ++ List("js/gears.js", "js/krona-2.0.js", "img/krona/loading.gif", "img/krona/hidden.png", "img/krona/favicon.ico")
    .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  def indexPage = {
    val krakenExecuted = summary.getStatsSize(runId = runId, pipeline = Right("gearskraken"), module = Some(Right("krakenreport")), library = Some(None), mustHaveSample = true) >= samples.size
    val centrifugeExecuted = summary.getStatsSize(runId, Right("gearscentrifuge"), Some(Right("centrifuge_report")), library = Some(None), mustHaveSample = true) >= samples.size
    val qiimeClosesOtuTable = summary.getFile(runId, Right("gears"), None, None, None, key = "qiime_closed_otu_table")
    val qiimeOpenOtuTable = summary.getFile(runId, Right("gears"), None, None, None, key = "qiime_open_otu_table")

    ReportPage(
      (if (centrifugeExecuted) List("Centriguge analysis" -> ReportPage(List("Non-unique" -> ReportPage(List(), List("All mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
        Map("summaryStatsTag" -> "centrifuge_report")
      )), Map())), List(
        "Unique mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
          Map("summaryStatsTag" -> "centrifuge_unique_report")
        )), Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge"))))
      else Nil) ::: (if (krakenExecuted) List("Kraken analysis" -> ReportPage(List(), List(
        "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
        )), Map()))
      else Nil) ::: (if (qiimeClosesOtuTable.isDefined) List("Qiime closed reference analysis" -> ReportPage(List(), List(
        "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
        )), Map("biomFile" -> qiimeClosesOtuTable.get)))
      else Nil) ::: (if (qiimeOpenOtuTable.isDefined) List("Qiime open reference analysis" -> ReportPage(List(), List(
        "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
        )), Map("biomFile" -> qiimeOpenOtuTable.get)))
      else Nil) ::: List("Samples" -> generateSamplesPage(pageArgs)) ++
        Map(
          "Versions" -> ReportPage(List(), List(
            "Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp")
          ), Map())
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
  def samplePage(sampleId: Int, args: Map[String, Any]): ReportPage = {
    val krakenExecuted = Await.result(summary.getStatsSize(runId, Right("gearskraken"), Some(Right("krakenreport")), sample = Left(sampleId), library = Some(None)), Duration.Inf) == 1
    val centrifugeExecuted = Await.result(summary.getStatsSize(runId, Right("gearscentrifuge"), Some(Right("centrifuge_report")), sample = Left(sampleId), library = None), Duration.Inf) == 1
    val qiimeClosesOtuTable = Await.result(summary.getFile(runId, Right("gears"), None, Some(Left(sampleId)), None, "qiime_closed_otu_table"), Duration.Inf)
    val qiimeOpenOtuTable = Await.result(summary.getFile(runId, Right("gears"), None, Some(Left(sampleId)), None, "qiime_open_otu_table"), Duration.Inf)

    ReportPage((if (centrifugeExecuted) List("Centriguge analysis" -> ReportPage(List("Non-unique" -> ReportPage(List(), List("All mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
      Map("summaryStatsTag" -> "centrifuge_report")
    )), Map())), List(
      "Unique mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
        Map("summaryStatsTag" -> "centrifuge_unique_report")
      )), Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge"))))
    else Nil) ::: (if (krakenExecuted) List("Kraken analysis" -> ReportPage(List(), List(
      "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
      )), Map()))
    else Nil) ::: (if (qiimeClosesOtuTable.isDefined) List("Qiime closed reference analysis" -> ReportPage(List(), List(
      "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
      )), Map("biomFile" -> qiimeClosesOtuTable.get)))
    else Nil) ::: (if (qiimeOpenOtuTable.isDefined) List("Qiime open reference analysis" -> ReportPage(List(), List(
      "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
      )), Map("biomFile" -> qiimeOpenOtuTable.get)))
    else Nil) ::: List(
      "Libraries" -> generateLibraryPage(args)
    ), List("QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
    ), args)
  }

  /** Library page */
  def libraryPage(sampleId: Int, libId: Int, args: Map[String, Any]): ReportPage = {
    val sName = Await.result(summary.getSampleName(sampleId), Duration.Inf)
    val lName = Await.result(summary.getLibraryName(libId), Duration.Inf)

    val flexiprepExecuted = Await.result(summary.getStatsSize(runId, Some(Right("flexiprep")), sample = Some(Left(sampleId)), library = Some(Left(libId))), Duration.Inf) >= 1

    val krakenExecuted = Await.result(summary.getStatsSize(runId, Right("gearskraken"), Some(Right("krakenreport")), sample = Some(Left(sampleId)), library = Some(Left(libId))), Duration.Inf) == 1
    val centrifugeExecuted = Await.result(summary.getStatsSize(runId, Right("gearscentrifuge"), Some(Right("centrifuge_report")), sample = Some(Left(sampleId)), library = Some(Left(libId))), Duration.Inf) == 1
    val qiimeClosesOtuTable = Await.result(summary.getFile(runId, Right("gears"), None, Some(Left(sampleId)), Some(Left(libId)), "qiime_closed_otu_table"), Duration.Inf)
    val qiimeOpenOtuTable = Await.result(summary.getFile(runId, Right("gears"), None, Some(Left(sampleId)), Some(Left(libId)), "qiime_open_otu_table"), Duration.Inf)

    ReportPage(
      (if (flexiprepExecuted) List("QC" -> FlexiprepReport.flexiprepPage) else Nil
      ) ::: (if (centrifugeExecuted) List("Centriguge analysis" -> ReportPage(List("Non-unique" -> ReportPage(List(), List("All mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
        Map("summaryStatsTag" -> "centrifuge_report")
      )), Map())), List(
        "Unique mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
          Map("summaryStatsTag" -> "centrifuge_unique_report")
        )), Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge"))))
      else Nil) ::: (if (krakenExecuted) List("Kraken analysis" -> ReportPage(List(), List(
        "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
        )), Map()))
      else Nil) ::: (if (qiimeClosesOtuTable.isDefined) List("Qiime closed reference analysis" -> ReportPage(List(), List(
        "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
        )), Map("biomFile" -> qiimeClosesOtuTable.get)))
      else Nil) ::: (if (qiimeOpenOtuTable.isDefined) List("Qiime open reference analysis" -> ReportPage(List(), List(
        "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/qiimeKrona.ssp"
        )), Map("biomFile" -> qiimeOpenOtuTable.get)))
      else Nil), List(
        "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
        "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
      ), args)
  }

}