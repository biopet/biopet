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

import nl.lumc.sasc.biopet.core.report.{ ReportSection, ReportPage, MultisampleReportBuilder, ReportBuilderExtension }
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Report for Gears
 *
 * Created by pjvan_thof on 12/10/15.
 */
class GearsReport(val root: Configurable) extends ReportBuilderExtension {
  def builder = GearsReport
}

object GearsReport extends MultisampleReportBuilder {

  def reportName = "Gears Report"

  override def extFiles = super.extFiles ++ List("js/gears.js", "js/krona-2.0.js", "img/krona/loading.gif", "img/krona/hidden.png", "img/krona/favicon.ico")
    .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  def indexPage = {
    val krakenExecuted = summary.getSampleValues("gearskraken", "stats", "krakenreport").values.forall(_.isDefined)
    val centrifugeExecuted = summary.getSampleValues("gearscentrifuge", "stats", "centrifuge_report").values.forall(_.isDefined)
    val qiimeClosesOtuTable = summary.getValue("gears", "files", "pipeline", "qiime_closed_otu_table", "path")
      .map(x => new File(x.toString))
    val qiimeOpenOtuTable = summary.getValue("gears", "files", "pipeline", "qiime_open_otu_table", "path")
      .map(x => new File(x.toString))

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
  def samplePage(sampleId: String, args: Map[String, Any]): ReportPage = {
    val krakenExecuted = summary.getValue(Some(sampleId), None, "gearskraken", "stats", "krakenreport").isDefined
    val centrifugeExecuted = summary.getValue(Some(sampleId), None, "gearscentrifuge", "stats", "centrifuge_report").isDefined
    val qiimeClosesOtuTable = summary.getValue(Some(sampleId), None, "gearsqiimeclosed", "files", "pipeline", "otu_table", "path")
      .map(x => new File(x.toString))
    val qiimeOpenOtuTable = summary.getValue(Some(sampleId), None, "gearsqiimeopen", "files", "pipeline", "otu_table", "path")
      .map(x => new File(x.toString))

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
  def libraryPage(sampleId: String, libId: String, args: Map[String, Any]): ReportPage = {
    val flexiprepExecuted = summary.getLibraryValue(sampleId, libId, "flexiprep").isDefined
    val krakenExecuted = summary.getValue(Some(sampleId), Some(libId), "gearskraken", "stats", "krakenreport").isDefined
    val centrifugeExecuted = summary.getValue(Some(sampleId), Some(libId), "gearscentrifuge", "stats", "centrifuge_report").isDefined
    val qiimeClosesOtuTable = summary.getValue(Some(sampleId), Some(libId), "gearsqiimeclosed", "files", "pipeline", "otu_table", "path")
      .map(x => new File(x.toString))
    val qiimeOpenOtuTable = summary.getValue(Some(sampleId), Some(libId), "gearsqiimeopen", "files", "pipeline", "otu_table", "path")
      .map(x => new File(x.toString))

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