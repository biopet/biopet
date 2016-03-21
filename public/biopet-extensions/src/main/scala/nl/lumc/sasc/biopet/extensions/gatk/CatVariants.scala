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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.{ Reference, BiopetJavaCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class CatVariants(val root: Configurable) extends BiopetJavaCommandLineFunction with Reference {

  javaMainClass = classOf[org.broadinstitute.gatk.tools.CatVariants].getClass.getName

  @Input(required = true)
  var inputFiles: List[File] = Nil

  @Output(required = true)
  var outputFile: File = null

  @Input
  var reference: File = null

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
  }

  override def cmdLine = super.cmdLine +
    repeat("-V", inputFiles) +
    required("-out", outputFile) +
    required("-R", reference)
}

object CatVariants {
  def apply(root: Configurable, input: List[File], output: File): CatVariants = {
    val cv = new CatVariants(root)
    cv.inputFiles = input
    cv.outputFile = output
    cv
  }
}