package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class Gzip(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Input file", required = true)
  var input: List[File] = Nil

  @Output(doc = "Unzipped file", required = true)
  var output: File = _

  executable = config("exe", default = "gzip")

  def cmdLine = required(executable) + " -c " + repeat(input) + " > " + required(output)
}

object Gzip {
  def apply(root: Configurable, input: List[File], output: File): Gzip = {
    val gzip = new Gzip(root)
    gzip.input = input
    gzip.output = output
    return gzip
  }
}