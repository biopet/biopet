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

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/** Wrapper for the bgzip command */
class Bgzip(val parent: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Input files", required = false)
  var input: List[File] = Nil

  @Output(doc = "Compressed output file", required = false)
  var output: File = null

  var f: Boolean = config("f", default = false)
  executable = config("exe", default = "bgzip", freeVar = false)

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (input.isEmpty && !inputAsStdin) Logging.addError("Input is missing for Bgzip")
    if (output == null && !outputAsStdout) Logging.addError("Output is missing for Bgzip")
  }

  def cmdLine =
    required(executable) +
      conditional(f, "-f") +
      " -c " + repeat(input) +
      (if (outputAsStdout) "" else " > " + required(output))
}
