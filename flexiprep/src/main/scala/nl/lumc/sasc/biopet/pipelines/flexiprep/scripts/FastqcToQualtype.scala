package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.function.PythonCommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class FastqcToQualtype(val globalConfig: Config) extends PythonCommandLineFunction {
  setPythonScript("__init__.py", "pyfastqc/")
  setPythonScript("qual_type_sickle.py")
  
  @Input(doc="Fastqc output", shortName="fastqc", required=true)
  var fastqc_output: File = _
  
  @Output(doc="Output file", shortName="out", required=true)
  var out: File = _
  
  def cmdLine = {
    getPythonCommand +
    required(fastqc_output.getParent()) +
    " > " +
    required(out)
  }
}