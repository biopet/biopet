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

  override def extFiles = super.extFiles ++ List("js/gears.js", "js/krona-2.0.js", "img/krona/loading.gif", "img/krona/hidden.png", "img/krona/favicon.ico")
    .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  /** Root page for the carp report */
  def indexPage = {

    val krakenExecuted = summary.getSampleValues("gearskraken", "stats", "krakenreport").values.forall(_.isDefined)
    val wgsExecuted = summary.getSampleValues("bammetrics", "stats", "wgs").values.exists(_.isDefined)
    val rnaExecuted = summary.getSampleValues("bammetrics", "stats", "rna").values.exists(_.isDefined)
    val insertsizeExecuted = summary.getSampleValues("bammetrics", "stats", "CollectInsertSizeMetrics", "metrics").values.exists(_ != Some(None))
    val mappingExecuted = summary.getLibraryValues("mapping").nonEmpty
    val pairedFound = !mappingExecuted || summary.getLibraryValues("mapping", "settings", "paired").exists(_._2 == Some(true))
    val flexiprepExecuted = summary.getLibraryValues("flexiprep")
      .exists { case ((sample, lib), value) => value.isDefined }

    ReportPage(
      List("Samples" -> generateSamplesPage(pageArgs)) ++
        (if (krakenExecuted) List("Dustbin analysis" -> ReportPage(List(), List(
          "Krona plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
          )), Map()))
        else Nil) ++
        List("Reference" -> ReportPage(List(), List(
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
        ),"Mapping Quality" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/mappingQuality.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)
        ),"Clipping" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/clipping.ssp",
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
    val krakenExecuted = summary.getValue(Some(sampleId), None, "gearskraken", "stats", "krakenreport").isDefined
    val flexiprepExecuted = summary.getLibraryValues("flexiprep")
      .exists { case ((sample, lib), value) => sample == sampleId && value.isDefined }

    ReportPage(List(
      "Libraries" -> generateLibraryPage(args),
      "Alignment" -> BammetricsReport.bamMetricsPage(summary, Some(sampleId), None)) ++
      (if (krakenExecuted) List("Dustbin analysis" -> ReportPage(List(), List(
        "Krona Plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
        )), Map()))
      else Nil) ++
      List("Files" -> filesPage
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
    val krakenExecuted = summary.getValue(Some(sampleId), Some(libId), "gearskraken", "stats", "krakenreport").isDefined
    val flexiprepExecuted = summary.getValue(Some(sampleId), Some(libId), "flexiprep").isDefined

    ReportPage(
      ("Alignment" -> BammetricsReport.bamMetricsPage(summary, Some(sampleId), Some(libId))) ::
        (if (flexiprepExecuted) List("QC" -> FlexiprepReport.flexiprepPage) else Nil) :::
        (if (krakenExecuted) List("Dustbin analysis" -> ReportPage(List(), List(
          "Krona Plot" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"
          )), Map()))
        else Nil),
      "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp") ::
        (if (flexiprepExecuted) List("QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
          "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp"))
        else Nil),
      args)
  }
}