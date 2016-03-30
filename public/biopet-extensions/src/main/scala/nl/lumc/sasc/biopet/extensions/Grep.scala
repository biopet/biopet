package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by pjvanthof on 30/03/16.
  */
class Grep(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Input file", required = true)
  var input: File = _

  @Output(doc = "Output file", required = true)
  var output: File = _

  executable = config("exe", default = "grep")

  var grepFor: String = null

  var invertMatch: Boolean = false
  var regex: Boolean = false
  var perlRegexp: Boolean = false

  /** return commandline to execute */
  def cmdLine = required(executable) +
    conditional(invertMatch, "-v") +
    conditional(regex, "-e") +
    conditional(perlRegexp, "-P") +
    required(grepFor) +
    (if (inputAsStdin) "" else required(input)) +
    (if (outputAsStsout) "" else " > " + required(output))
}

object Grep {
  def apply(root: Configurable,
            grepFor: String,
            regex: Boolean = false,
            invertMatch: Boolean = false,
            perlRegexp: Boolean = false): Grep = {
    val grep  = new Grep(root)
    grep.grepFor = grepFor
    grep.regex = regex
    grep.perlRegexp = perlRegexp
    grep.invertMatch = invertMatch
    grep
  }
}