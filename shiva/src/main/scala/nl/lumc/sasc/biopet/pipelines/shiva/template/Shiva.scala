package nl.lumc.sasc.biopet.pipelines.shiva.template

import nl.lumc.sasc.biopet.core.{Reference, TemplateTool}

/**
  * Created by pjvanthof on 17/12/2016.
  */
object Shiva extends TemplateTool {
  def pipelineMap(map: Map[String, Any], expert: Boolean): Map[String, Any] = {
    map ++ Reference.askReference
  }
}
