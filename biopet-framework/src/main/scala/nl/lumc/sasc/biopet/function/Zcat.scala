package nl.lumc.sasc.biopet.function

import nl.lumc.sasc.biopet.core._
//import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class Zcat(val globalConfig: Config) extends BiopetCommandLineFunction {
  @Input(doc="Zipped file")
  var input: File = _
  
  @Output(doc="Unzipped file")
  var output: File = _
  
  executeble = config.getAsString("exe", "zcat")
  
  def cmdLine = required(executeble) + required(input) + " > " + required(output)
}