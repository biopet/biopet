package nl.lumc.sasc.biopet.pipelines.tinycap

import nl.lumc.sasc.biopet.core.report.{ ReportBuilderExtension, ReportSection }
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

  override def additionalSections = List(
    "Fragments per gene" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gentrap/measure_plotreport.ssp",
      Map("pipelineName" -> pipelineName,
        "plotName" -> "fragmentspergene",
        "plotPath" -> summary.getValue("fragmentspergene", "files", "pipeline", "fragments_per_gene_heatmap", "path")
      )),
    "Fragments per microRNA" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gentrap/measure_plotreport.ssp",
      Map("pipelineName" -> pipelineName,
        "plotName" -> "fragmentspersmallrna",
        "plotPath" -> summary.getValue("fragmentspersmallrna", "files", "pipeline", "fragments_per_smallrna_heatmap", "path")))
  )

  override def pipelineName = "tinycap"
}
