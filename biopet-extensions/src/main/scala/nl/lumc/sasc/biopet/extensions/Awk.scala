package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

/**
  * Created by pjvan_thof on 17-5-16.
  */
class Awk(val root: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "awk", freeVar = false)

  def versionCommand: String = executable + " --version"

  def versionRegex: Regex = """(GNU Awk \d+\.\d+\.\d+)""".r

  @Input(required = false)
  var input: File = _

  @Output
  var output: File = _

  var command: String = _

  def cmdLine = executable +
    required(command) +
    (if (inputAsStdin) "" else required(input)) +
    (if (outputAsStsout) "" else " > " + required(output))
}

object Awk {
  def apply(root: Configurable, command: String): Awk = {
    val awk = new Awk(root)
    awk.command = command
    awk
  }
}