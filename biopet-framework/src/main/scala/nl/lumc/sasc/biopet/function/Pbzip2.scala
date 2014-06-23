package nl.lumc.sasc.biopet.function

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
//import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class Pbzip2(val root:Configurable) extends BiopetCommandLineFunction {
  @Input(doc="Zipped file")
  var input: File = _
  
  @Output(doc="Unzipped file")
  var output: File = _
  
  executeble = config("exe", "pbzip2")
  
  var decomrpess = true
  var memory: Int = config("memory", 1000)
  
  override val defaultVmem = (memory * 2 / 1000) + "G"
  override val defaultThreads = 2
  
  override def beforeCmd {
    memory = memory * threads
  }
  
  def cmdLine = required(executeble) +
      conditional(decomrpess, "-d") +
      conditional(!decomrpess, "-z") +
      optional("-p", threads, spaceSeparated=false) +
      optional("-m", memory, spaceSeparated=false) +
      required("-c", output) +
      required(input)
}

object Pbzip2 {
  def apply(root:Configurable, input:File, output:File): Pbzip2 = {
    val pbzip2 = new Pbzip2(root)
    pbzip2.input = input
    pbzip2.output = output
    return pbzip2
  }
}