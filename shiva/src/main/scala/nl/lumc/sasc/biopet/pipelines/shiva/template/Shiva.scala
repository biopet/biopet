package nl.lumc.sasc.biopet.pipelines.shiva.template

import java.io.File

import nl.lumc.sasc.biopet.core.{Reference, TemplateTool}
import nl.lumc.sasc.biopet.utils.Question

/**
  * Created by pjvanthof on 17/12/2016.
  */
object Shiva extends TemplateTool {

  override val sampleConfigs: List[File] = TemplateTool.askSampleConfigs()

  def pipelineMap(map: Map[String, Any], expert: Boolean): Map[String, Any] = {
    map ++ Reference.askReference ++
      Map("variantcallers" -> Question.list("Variantcallers",
        posibleValues = List("haplotypercaller", "haplotypecaller_gvcf")))
  }
}
