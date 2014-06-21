package nl.lumc.sasc.biopet.function

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import org.broadinstitute.sting.commandline._
import java.io.File

class Sha1sum(val globalConfig: Config, val configPath: List[String]) extends BiopetCommandLineFunction {
  @Input(doc="Zipped file")
  var input: File = _
  
  @Output(doc="Unzipped file")
  var output: File = _
  
  executeble = config("exe","sha1sum")
  
  def cmdLine = required(executeble) + required(input) + " > " + required(output)
}