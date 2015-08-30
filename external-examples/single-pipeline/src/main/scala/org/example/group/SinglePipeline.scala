package org.example.group

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.pipelines.shiva.Shiva
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 8/28/15.
 */
class SinglePipeline(val root: Configurable) extends QScript with SummaryQScript {
  def this() = this(null)

  def summaryFile = new File(outputDir, "magpie.summary.json")

  def summaryFiles: Map[String, File] = Map()

  def summarySettings = Map()

  def init(): Unit = {
  }

  def biopetScript: Unit = {
    val shiva = new Shiva(this)
    shiva.init()
    shiva.biopetScript()
    addAll(shiva.functions)

    addSummaryQScript(shiva)
  }
}

object SinglePipeline extends PipelineCommand
