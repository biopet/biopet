package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Version }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.util.matching.Regex

/**
 * Created by pjvanthof on 18/05/16.
 */
class Sed(val root: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "sed", freeVar = false)

  /** Command to get version of executable */
  override def versionCommand: String = executable + " --version"

  /** Regex to get version from version command output */
  override def versionRegex: Regex = """sed (GNU sed) \d+.\d+.\d+""".r

  @Input(required = false)
  var inputFile: File = _

  @Output
  var outputFile: File = _

  var expressions: List[String] = Nil

  def cmdLine = executable +
    repeat("-e", expressions) +
    (if (inputAsStdin) "" else required(inputFile)) +
    (if (outputAsStsout) "" else " > " + required(outputFile))

}
