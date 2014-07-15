package nl.lumc.sasc.biopet.function

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import org.broadinstitute.gatk.utils.commandline._
import java.io.File

class Cat(val root:Configurable) extends BiopetCommandLineFunction {
  @Input(doc="Input file", required=true)
  var input: List[File] = Nil
  
  @Output(doc="Unzipped file", required=true)
  var output: File = _
  
  executeble = config("exe", "cat")
  
  def cmdLine = required(executeble) + repeat(input) + " > " + required(output)
}

object Cat {
  def apply(root:Configurable, input:List[File], output:File): Cat = {
    val cat = new Cat(root)
    cat.input = input
    cat.output = output
    return cat
  }
}