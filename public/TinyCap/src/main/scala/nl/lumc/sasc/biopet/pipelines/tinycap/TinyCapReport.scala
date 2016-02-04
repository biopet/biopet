package nl.lumc.sasc.biopet.pipelines.tinycap

import nl.lumc.sasc.biopet.core.report.{ReportPage, ReportSection, ReportBuilderExtension}
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingReportTrait
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
  * Created by wyleung on 4-2-16.
  */
class TinyCapReport(val root: Configurable) extends ReportBuilderExtension {
  def builder = TinyCapReport

}

object TinyCapReport extends MultisampleMappingReportTrait {
  /** Name of the report */
  def reportName = "TinyCap Report"

  /** Front section for the report */
  override def frontSection: ReportSection = ReportSection("/nl/lumc/sasc/biopet/pipelines/tinycap/tinycapFront.ssp")

  override def pipelineName = "tinycap"


  /** Root page for the carp report */
  override def indexPage = {

    val wgsExecuted = summary.getSampleValues("bammetrics", "stats", "wgs").values.exists(_.isDefined)
    val rnaExecuted = summary.getSampleValues("bammetrics", "stats", "rna").values.exists(_.isDefined)

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
        "Report" -> frontSection,
        "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)
        )) ++
        (if (wgsExecuted) List("Whole genome coverage" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/wgsHistogram.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)))
        else Nil) ++
        (if (rnaExecuted) List("Rna coverage" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/rnaHistogram.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)))
        else Nil) ++
        List("QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp",
          Map("showPlot" -> true, "showTable" -> false)),
          "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp",
            Map("showPlot" -> true, "showTable" -> false))
        ),
      pageArgs ++ Map("pipeline" -> pipelineName)
    )
  }



}
