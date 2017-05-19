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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by pjvanthof on 20/06/16.
  */
class GffRead(val parent: Configurable) extends BiopetCommandLineFunction {

  executable = config("exe", default = "gffread", freeVar = false)

  @Input
  var input: File = _

  @Output
  var output: File = _

  var T: Boolean = config("T", default = false, freeVar = false)

  def cmdLine =
    executable +
      (if (inputAsStdin) "" else required(input)) +
      (if (outputAsStdout) "" else required("-o", output)) +
      conditional(T, "-T")
}
