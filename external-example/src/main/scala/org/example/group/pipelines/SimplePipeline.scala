package org.example.group.pipelines

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.{ Gzip, Cat }
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 30/08/15.
 */
class SimplePipeline(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(required = true)
  var inputFile: File = null

  /** This method can be used to initialize some classes where needed */
  def init(): Unit = {
  }

  /** This method is the actual pipeline */
  def biopetScript: Unit = {
    val cat = new Cat(this)
    cat.input :+= inputFile
    cat.output = new File(outputDir, "file.out")
    add(cat)

    val gzip = new Gzip(this)
    gzip.input :+= cat.output
    gzip.output = new File(outputDir, "file.out.gz")
    add(gzip)
  }
}

//TODO: Replace object Name, must be the same as the class of the pipeline
object SimplePipeline extends PipelineCommand
