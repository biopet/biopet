package nl.lumc.sasc.biopet.extensions.titan

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

class Titan(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Input files", required = true)
  var input: List[File] = Nil

  @Output(doc = "Compressed output file", required = true)
  var output: File = null

  var f: Boolean = config("f", default = false)
  executable = config("exe", default = "bgzip")

  def cmdLine = required(executable) +
    conditional(f, "-f") +
    " -c " + repeat(input) +
    " > " + required(output)
}
