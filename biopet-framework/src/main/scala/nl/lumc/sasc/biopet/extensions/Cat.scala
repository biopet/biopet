package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline._
import java.io.File

class Cat(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Input file", required = true)
  var input: List[File] = Nil

  @Output(doc = "Unzipped file", required = true)
  var output: File = _

  executable = config("exe", default = "cat")

  def cmdLine = required(executable) + repeat(input) + " > " + required(output)
}

object Cat {
  def apply(root: Configurable, input: List[File], output: File): Cat = {
    val cat = new Cat(root)
    cat.input = input
    cat.output = output
    return cat
  }
}