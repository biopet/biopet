package nl.lumc.sasc.biopet.function

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.commandline._
import java.io.File

class Sha1sum(val globalConfig: Config) extends BiopetCommandLineFunction {
  @Input(doc="Zipped file")
  var input: File = _
  
  @Output(doc="Unzipped file")
  var output: File = _
  
  executeble = config.getAsString("exe","sha1sum")
  
  def cmdLine = required(executeble) + required(input) + " > " + required(output)
}