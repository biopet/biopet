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
package org.example.group.pipelines

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.extensions.{ Gzip, Cat }
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 30/08/15.
 */
//TODO: Replace class name, must be the same as the class of the pipeline
class SimplePipeline(val parent: Configurable) extends QScript with BiopetQScript {
  // A constructor without arguments is needed if this pipeline is a root pipeline
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

//TODO: Replace object name, must be the same as the class of the pipeline
object SimplePipeline extends PipelineCommand
