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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.{Reference, ToolCommandFunction}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

class ValidateAnnotation(val parent: Configurable) extends ToolCommandFunction with Reference {
  def toolObject = nl.lumc.sasc.biopet.tools.ValidateAnnotation

  @Input(required = true)
  var refflatFile: File = _

  @Input(required = false)
  var gtfFile: List[File] = Nil

  @Input(required = true)
  var reference: File = _

  var disableFail: Boolean = false

  override def defaultCoreMemory = 16.0

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
  }

  override def cmdLine =
    super.cmdLine +
      required("-r", refflatFile) +
      repeat("-g", gtfFile) +
      required("-R", reference) +
      conditional(disableFail, "--disableFail")
}
