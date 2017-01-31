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
package nl.lumc.sasc.biopet.pipelines.shiva.template

import java.io.File

import nl.lumc.sasc.biopet.core.TemplateTool
import nl.lumc.sasc.biopet.pipelines.mapping.template.MultiSampleMapping
import nl.lumc.sasc.biopet.pipelines.shiva.ShivaVariantcalling
import nl.lumc.sasc.biopet.utils.Question

/**
 * Created by pjvanthof on 17/12/2016.
 */
object Shiva extends TemplateTool {

  def pipelineName = "Shiva"

  override def sampleConfigs: List[File] = TemplateTool.askSampleConfigs()

  def possibleVariantcallers: List[String] = {
    ShivaVariantcalling.callersList(null).map(_.name)
  }

  def pipelineMap(map: Map[String, Any], expert: Boolean): Map[String, Any] = {
    val mappingConfig = MultiSampleMapping.pipelineMap(map, expert)

    val variantCallers = Question.list("Variantcallers", posibleValues = possibleVariantcallers,
      default = Some(List("haplotypecaller_gvcf", "haplotypecaller")))
    val useIndelRealigner = Question.boolean("Use indel realigner", default = Some(true))
    val useBaseRecalibration = Question.boolean("Use base recalibration", default = Some(true))

    mappingConfig ++ Map(
      "variantcallers" -> variantCallers,
      "use_indel_realigner" -> useIndelRealigner,
      "use_base_recalibration" -> useBaseRecalibration
    )
  }
}
