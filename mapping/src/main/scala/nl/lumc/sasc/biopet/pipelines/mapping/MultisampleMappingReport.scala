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

import nl.lumc.sasc.biopet.core.report.{ MultisampleReportBuilder, ReportBuilderExtension, ReportPage, ReportSection }
import nl.lumc.sasc.biopet.pipelines.bammetrics.BammetricsReport
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by pjvanthof on 11/01/16.
 */
class MultisampleMappingReport(val parent: Configurable) extends ReportBuilderExtension {
  def builder = MultisampleMappingReport
}

object MultisampleMappingReport extends MultisampleMappingReportTrait {
  /** Name of the report */
  def reportName = "Mapping Report"
}

trait MultisampleMappingReportTrait extends MultisampleReportBuilder {
  /** Front section for the report */
  def frontSection: ReportSection = ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/multisampleMappingFront.ssp")

  def additionalSections: List[(String, ReportSection)] = Nil

  def pipelineName = "multisamplemapping"

  override def extFiles = super.extFiles ++ List("js/gears.js", "js/krona-2.0.js", "img/krona/loading.gif", "img/krona/hidden.png", "img/krona/favicon.ico")
    .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  /** Root page for the carp report */
  def indexPage = {

    val krakenExecuted = Await.result(summary.getStatsSize(runId, "gearskraken", "krakenreport",
      library = NoLibrary, mustHaveSample = true), Duration.Inf) >= 1
    val centrifugeExecuted = Await.result(summary.getStatsSize(runId, "gearscentrifuge", "centrifuge_report",
      library = NoLibrary, mustHaveSample = true), Duration.Inf) >= 1
    val wgsExecuted = Await.result(summary.getStatsSize(runId, "bammetrics", "wgs",
      library = NoLibrary, mustHaveSample = true), Duration.Inf) >= 1
    val rnaExecuted = Await.result(summary.getStatsSize(runId, "bammetrics", "rna",
      library = NoLibrary, mustHaveSample = true), Duration.Inf) >= 1
    val insertsizeExecuted = summary.getStatsForSamples(runId, "bammetrics", "CollectInsertSizeMetrics", keyValues = Map("metrics" -> List("metrics"))).exists(_._2("metrics").isDefined)
    val mappingExecuted = Await.result(summary.getStatsSize(runId, "mapping", NoModule, mustHaveLibrary = true), Duration.Inf) >= 1
    val mappingSettings = summary.getSettingsForLibraries(runId, "mapping", NoModule, keyValues = Map("paired" -> List("paired")))
    val pairedFound = !mappingExecuted || mappingSettings.exists(_._2.exists(_._2 == Option(true)))
    val flexiprepExecuted = Await.result(summary.getStatsSize(runId, "flexiprep", mustHaveLibrary = true), Duration.Inf) >= 1

    ReportPage(
      List("Samples" -> generateSamplesPage(pageArgs)) ++
        (if (krakenExecuted) List("Dustbin analysis - Kraken" -> ReportPage(List(), List(
          "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
          )), Map()))
        else Nil) ++ (if (centrifugeExecuted) List("Centriguge analysis" -> ReportPage(List("Non-unique" -> ReportPage(List(), List("All mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
          Map("summaryStatsTag" -> "centrifuge_report")
        )), Map())), List(
          "Unique mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
            Map("summaryStatsTag" -> "centrifuge_unique_report")
          )), Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge"))))
        else Nil) ++
        List("Reference" -> ReportPage(List(), List(
          "Reference" -> ReportSection("/nl/lumc/sasc/biopet/core/report/reference.ssp", Map("pipeline" -> pipelineName))
        ), Map()),
          "Files" -> Await.result(filesPage(), Duration.Inf),
          "Versions" -> ReportPage(List(), List("Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp"
          )), Map())
        ),
      List(
        "Report" -> frontSection) ++
        additionalSections ++
        List("Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)
        ), "Mapping Quality" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/mappingQuality.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)
        ), "Clipping" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/clipping.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)
        )) ++
        (if (pairedFound && insertsizeExecuted) List("Insert Size" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/insertSize.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)))
        else Nil) ++
        (if (wgsExecuted) List("Whole genome coverage" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/wgsHistogram.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)))
        else Nil) ++
        (if (rnaExecuted) List("Rna coverage" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/rnaHistogram.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)))
        else Nil) ++
        (if (flexiprepExecuted) List("QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp",
          Map("showPlot" -> true, "showTable" -> false)),
          "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp",
            Map("showPlot" -> true, "showTable" -> false))
        )
        else Nil),
      pageArgs ++ Map("pipeline" -> pipelineName)
    )
  }

  /** Files page, can be used general or at sample level */
  def filesPage(sampleId: Option[Int] = None, libraryId: Option[Int] = None): Future[ReportPage] = {
    val dbFiles = summary.getFiles(runId, sample = sampleId.map(SampleId),
      library = libraryId.map(LibraryId))
      .map(_.groupBy(_.pipelineId))
    val modulePages = dbFiles.map(_.map {
      case (pipelineId, files) =>
        val moduleSections = files.groupBy(_.moduleId).map {
          case (moduleId, files) =>
            val moduleName: Future[String] = moduleId match {
              case Some(id) => summary.getModuleName(pipelineId, id).map(_.getOrElse("Pipeline"))
              case _        => Future("Pipeline")
            }
            moduleName.map(_ -> ReportSection("/nl/lumc/sasc/biopet/core/report/files.ssp", Map("files" -> files)))
        }
        val moduleSectionsSorted = moduleSections.find(_._1 == "Pipeline") ++ moduleSections.filter(_._1 != "Pipeline")
        summary.getPipelineName(pipelineId = pipelineId).map(_.get -> ReportPage(Nil, Await.result(Future.sequence(moduleSectionsSorted), Duration.Inf).toList, Map()))
    })

    val pipelineFiles = summary.getPipelineId(runId, pipelineName).flatMap(pipelinelineId => dbFiles.map(x => x(pipelinelineId.get).filter(_.moduleId.isEmpty)))

    modulePages.flatMap(Future.sequence(_)).map(x => ReportPage(x.toList,
      s"$pipelineName files" -> ReportSection("/nl/lumc/sasc/biopet/core/report/files.ssp", Map("files" -> Await.result(pipelineFiles, Duration.Inf))) ::
        "Sub pipelines/modules" -> ReportSection("/nl/lumc/sasc/biopet/core/report/fileModules.ssp", Map("pipelineIds" -> Await.result(dbFiles.map(_.keys.toList), Duration.Inf))) :: Nil, Map()))
  }

  /** Single sample page */
  def samplePage(sampleId: Int, args: Map[String, Any]): ReportPage = {
    val krakenExecuted = Await.result(summary.getStatsSize(runId, "gearskraken", "krakenreport",
      library = NoLibrary, sample = sampleId), Duration.Inf) >= 1
    val centrifugeExecuted = Await.result(summary.getStatsSize(runId, "gearscentrifuge", "centrifuge_report",
      library = NoLibrary, sample = sampleId, mustHaveSample = true), Duration.Inf) >= 1
    val flexiprepExecuted = Await.result(summary.getStatsSize(runId, "flexiprep",
      sample = sampleId, mustHaveLibrary = true), Duration.Inf) >= 1

    ReportPage(List(
      "Libraries" -> generateLibraryPage(args),
      "Alignment" -> BammetricsReport.bamMetricsPage(summary, Some(sampleId), None)) ++
      (if (centrifugeExecuted) List("Centriguge analysis" -> ReportPage(List("Non-unique" -> ReportPage(List(), List("All mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
        Map("summaryStatsTag" -> "centrifuge_report", "centrifugeTag" -> Some("centrifuge"))
      )), Map())), List(
        "Unique mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
          Map("summaryStatsTag" -> "centrifuge_unique_report")
        )), Map("summaryModuleTag" -> "gearscentrifuge")))
      else Nil) ::: (if (krakenExecuted) List("Dustbin analysis" -> ReportPage(List(), List(
        "Krona Plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
        )), Map()))
      else Nil) ++
      List("Files" -> Await.result(filesPage(sampleId = sampleId), Duration.Inf)
      ), List(
      "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp",
        Map("showPlot" -> true)),
      "Preprocessing" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp", Map("sampleLevel" -> true))) ++
      (if (flexiprepExecuted) List("QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
        "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
      )
      else Nil), args)
  }

  /** Library page */
  def libraryPage(sampleId: Int, libId: Int, args: Map[String, Any]): ReportPage = {
    val krakenExecuted = Await.result(summary.getStatsSize(runId, "gearskraken", "krakenreport",
      library = libId, sample = sampleId), Duration.Inf) >= 1
    val centrifugeExecuted = Await.result(summary.getStatsSize(runId, "gearscentrifuge", "centrifuge_report",
      library = libId, sample = sampleId, mustHaveSample = true), Duration.Inf) >= 1
    val flexiprepExecuted = Await.result(summary.getStatsSize(runId, "flexiprep", library = libId,
      sample = sampleId, mustHaveLibrary = true), Duration.Inf) >= 1

    ReportPage(
      ("Alignment" -> BammetricsReport.bamMetricsPage(summary, Some(sampleId), Some(libId))) ::
        (if (flexiprepExecuted) List("QC" -> FlexiprepReport.flexiprepPage) else Nil) :::
        (if (centrifugeExecuted) List("Centriguge analysis" -> ReportPage(List("Non-unique" -> ReportPage(List(), List("All mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
          Map("summaryStatsTag" -> "centrifuge_report")
        )), Map())), List(
          "Unique mappings" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
            Map("summaryStatsTag" -> "centrifuge_unique_report")
          )), Map("summaryModuleTag" -> "gearscentrifuge", "centrifugeTag" -> Some("centrifuge"))))
        else Nil) ::: (if (krakenExecuted) List("Dustbin analysis" -> ReportPage(List(), List(
          "Krona Plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
          )), Map()))
        else Nil) ::: List("Files" -> Await.result(filesPage(sampleId = sampleId, libraryId = libId), Duration.Inf)),
      "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp") ::
        (if (flexiprepExecuted) List("QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
          "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp"))
        else Nil),
      args)
  }
}