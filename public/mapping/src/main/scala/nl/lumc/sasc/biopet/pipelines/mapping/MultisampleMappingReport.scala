package nl.lumc.sasc.biopet.pipelines.mapping

import nl.lumc.sasc.biopet.core.report.{ ReportBuilderExtension, ReportSection, ReportPage, MultisampleReportBuilder }
import nl.lumc.sasc.biopet.pipelines.bammetrics.BammetricsReport
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by pjvanthof on 11/01/16.
 */
class MultisampleMappingReport(val root: Configurable) extends ReportBuilderExtension {
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

  /** Root page for the carp report */
  def indexPage = {

    val wgsExecuted = summary.getSampleValues("bammetrics", "stats", "wgs").values.exists(_.isDefined)
    val rnaExecuted = summary.getSampleValues("bammetrics", "stats", "rna").values.exists(_.isDefined)
    val insertsizeExecuted = summary.getSampleValues("bammetrics", "stats", "CollectInsertSizeMetrics", "metrics").values.exists(_ != Some(None))
    val flexiprepExecuted = summary.getLibraryValues("flexiprep")
      .exists { case ((sample, lib), value) => value.isDefined }

    ReportPage(
      List("Samples" -> generateSamplesPage(pageArgs)) ++
        Map("Reference" -> ReportPage(List(), List(
          "Reference" -> ReportSection("/nl/lumc/sasc/biopet/core/report/reference.ssp", Map("pipeline" -> pipelineName))
        ), Map()),
          "Files" -> filesPage,
          "Versions" -> ReportPage(List(), List("Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp"
          )), Map())
        ),
      List(
        "Report" -> frontSection) ++
        additionalSections ++
        List("Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)
        )) ++
        (if (insertsizeExecuted) List("Insert Size" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/insertSize.ssp",
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
  def filesPage: ReportPage = {
    val flexiprepExecuted = summary.getLibraryValues("flexiprep")
      .exists { case ((sample, lib), value) => value.isDefined }

    ReportPage(List(), (if (flexiprepExecuted) List(
      "Input fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepInputfiles.ssp"),
      "After QC fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepOutputfiles.ssp"))
    else Nil) :::
      List("Bam files per lib" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/outputBamfiles.ssp", Map("sampleLevel" -> false)),
        "Preprocessed bam files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/outputBamfiles.ssp",
          Map("pipelineName" -> pipelineName, "fileTag" -> "output_bam_preprocess"))), Map())
  }

  /** Single sample page */
  def samplePage(sampleId: String, args: Map[String, Any]): ReportPage = {
    val flexiprepExecuted = summary.getLibraryValues("flexiprep")
      .exists { case ((sample, lib), value) => sample == sampleId && value.isDefined }

    ReportPage(List(
      "Libraries" -> generateLibraryPage(args),
      "Alignment" -> BammetricsReport.bamMetricsPage(summary, Some(sampleId), None),
      "Files" -> filesPage
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
  def libraryPage(sampleId: String, libId: String, args: Map[String, Any]): ReportPage = {
    val flexiprepExecuted = summary.getValue(Some(sampleId), Some(libId), "flexiprep").isDefined

    ReportPage(
      ("Alignment" -> BammetricsReport.bamMetricsPage(summary, Some(sampleId), Some(libId))) ::
        (if (flexiprepExecuted) List("QC" -> FlexiprepReport.flexiprepPage) else Nil),
      "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp") ::
        (if (flexiprepExecuted) List("QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
          "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp"))
        else Nil),
      args)
  }
}