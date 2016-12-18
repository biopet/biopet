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

  override lazy val sampleConfigs: List[File] = TemplateTool.askSampleConfigs()

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
