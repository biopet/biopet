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

import scala.util.matching.Regex

/**
  * Created by pjvan_thof on 17-5-16.
  */
class Awk(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "awk", freeVar = false)

  def versionCommand: String = executable + " --version"

  def versionRegex: List[Regex] = """(GNU Awk \d+\.\d+\.\d+)""".r :: Nil

  @Input(required = false)
  var input: File = _

  @Output
  var output: File = _

  var command: String = _

  def cmdLine: String =
    executable +
      required(command) +
      (if (inputAsStdin) "" else required(input)) +
      (if (outputAsStdout) "" else " > " + required(output))
}

object Awk {
  def apply(root: Configurable, command: String): Awk = {
    val awk = new Awk(root)
    awk.command = command
    awk
  }
}
