package nl.lumc.sasc.biopet.pipelines.shiva.template

import java.io.File

import nl.lumc.sasc.biopet.core.{Reference, TemplateTool}
import nl.lumc.sasc.biopet.pipelines.shiva.ShivaVariantcalling
import nl.lumc.sasc.biopet.utils.Question

/**
  * Created by pjvanthof on 17/12/2016.
  */
object Shiva extends TemplateTool {

  override val sampleConfigs: List[File] = TemplateTool.askSampleConfigs()

  def possibleVariantcallers: List[String] = {
    ShivaVariantcalling.callersList(null).map(_.name)
  }

  def pipelineMap(map: Map[String, Any], expert: Boolean): Map[String, Any] = {
    map ++ Reference.askReference ++
      Map(
        "variantcallers" -> Question.list("Variantcallers", posibleValues = possibleVariantcallers,
          default = Some(List("unifiedgenotyper", "haplotypecaller_gvcf", "haplotypecaller"))),
        "use_indel_realigner" -> Question.boolean("use_indel_realigner", default = Some(true)),
        "use_base_recalibration" -> Question.boolean("use_base_recalibration", default = Some(true))
      )
  }
}
