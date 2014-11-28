package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class Zcat(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Zipped file")
  var input: File = _

  @Output(doc = "Unzipped file")
  var output: File = _

  executable = config("exe", default = "zcat")

  def cmdLine = required(executable) + required(input) + " > " + required(output)
}

object Zcat {
  def apply(root: Configurable, input: File, output: File): Zcat = {
    val zcat = new Zcat(root)
    zcat.input = input
    zcat.output = output
    return zcat
  }
}