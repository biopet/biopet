package org.example.group.pipelines

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.pipelines.shiva.Shiva
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 8/28/15.
 */
//TODO: Replace class Name
class BiopetPipeline(val root: Configurable) extends QScript with SummaryQScript {
  def this() = this(null)

  /** Only required when using [[SummaryQScript]] */
  def summaryFile = new File(outputDir, "magpie.summary.json")

  /** Only required when using [[SummaryQScript]] */
  def summaryFiles: Map[String, File] = Map()

  /** Only required when using [[SummaryQScript]] */
  def summarySettings = Map()

  // This method can be used to initialize some classes where needed
  def init(): Unit = {
  }

  // This method is the actual pipeline
  def biopetScript: Unit = {

    // Executing a biopet pipeline inside
    val shiva = new Shiva(this)
    add(shiva)

    shiva.init()
    shiva.biopetScript()
    addAll(shiva.functions)

    /* Only required when using [[SummaryQScript]] */
    addSummaryQScript(shiva)

    // From here you can use the output files of shiva as input file of other jobs
  }
}

//TODO: Replace object Name, must be the same as the class of the pipeline
object BiopetPipeline extends PipelineCommand
