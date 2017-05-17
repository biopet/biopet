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

import nl.lumc.sasc.biopet.core.{Version, BiopetCommandLineFunction}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

class Gzip(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  @Input(doc = "Input file", required = true)
  var input: List[File] = Nil

  @Output(doc = "Unzipped file", required = true)
  var output: File = _

  executable = config("exe", default = "gzip", freeVar = false)

  def versionRegex = """gzip (.*)""".r
  def versionCommand = executable + " --version"

  def cmdLine =
    required(executable) + " -c " +
      (if (inputAsStdin) "" else repeat(input)) +
      (if (outputAsStdout) "" else " > " + required(output))
}

object Gzip {
  def apply(root: Configurable): Gzip = new Gzip(root)

  def apply(root: Configurable, input: File, output: File): Gzip = Gzip(root, List(input), output)

  def apply(root: Configurable, input: List[File], output: File): Gzip = {
    val gzip = new Gzip(root)
    gzip.input = input
    gzip.output = output
    gzip
  }
}
