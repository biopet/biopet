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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.mapping

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.report.{ ReportBuilderExtension, ReportSection, ReportPage, ReportBuilder }
import nl.lumc.sasc.biopet.pipelines.bammetrics.BammetricsReport
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport

class MappingReport(val root: Configurable) extends ReportBuilderExtension {
  val builder = MappingReport
}

/**
 * Object ot generate report for [[Mapping]]
 *
 * Created by pjvanthof on 24/06/15.
 */
object MappingReport extends ReportBuilder {
  /** Name of report */
  val reportName = "Mapping Report"

  /** Root page for single BamMetrcis report */
  def indexPage = {
    val skipFlexiprep = summary.getValue(sampleId, libId, "mapping", "settings", "skip_flexiprep").getOrElse(false) == true
    val bamMetricsPage = if (summary.getValue(sampleId, libId, "mapping", "settings", "skip_metrics").getOrElse(false) != true) {
      Some(BammetricsReport.bamMetricsPage(summary, sampleId, libId))
    } else None
    ReportPage((if (skipFlexiprep) Nil else List("QC" -> FlexiprepReport.flexiprepPage)) :::
      bamMetricsPage.map(_.subPages).getOrElse(Nil) ::: List(
        "Versions" -> ReportPage(List(), List("Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp"
        )), Map()),
        "Files" -> ReportPage(List(), (if (skipFlexiprep) Nil else List(
          "Input fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepInputfiles.ssp"),
          "After QC fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepOutputfiles.ssp"))) :::
          List("Bam files per lib" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/outputBamfiles.ssp", Map("sampleLevel" -> false))
          ), Map())
      ), List(
      "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/mappingFront.ssp")
    ) ::: bamMetricsPage.map(_.sections).getOrElse(Nil),
      Map()
    )
  }
}
