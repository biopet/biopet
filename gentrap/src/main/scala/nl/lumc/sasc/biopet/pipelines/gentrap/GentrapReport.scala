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
package nl.lumc.sasc.biopet.pipelines.gentrap

import nl.lumc.sasc.biopet.core.report.{ ReportBuilderExtension, ReportSection }
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingReportTrait
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Class to generate report for [[Gentrap]]
 *
 * Created by pjvanthof on 25/06/15.
 */
class GentrapReport(val parent: Configurable) extends ReportBuilderExtension {
  def builder = GentrapReport
}

object GentrapReport extends MultisampleMappingReportTrait {

  override def frontSection = ReportSection("/nl/lumc/sasc/biopet/pipelines/gentrap/gentrapFront.ssp")

  /** Name of the report */
  def reportName = "Gentrap Report"

  override def pipelineName = "gentrap"
}