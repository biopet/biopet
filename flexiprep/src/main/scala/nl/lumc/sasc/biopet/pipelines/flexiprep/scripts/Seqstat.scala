package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.function.PythonCommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class Seqstat(val globalConfig: Config) extends PythonCommandLineFunction {
  setPythonScript("__init__.py", "pyfastqc/")
  setPythonScript("seq_stat.py")
  
  @Input(doc="Fastq input", shortName="fastqc", required=true) var input_fastq: File = _
  @Output(doc="Output file", shortName="out", required=true) var out: File = _
  
  var fmt: String = _
  
  def cmdLine = {
    getPythonCommand + 
    optional("--fmt", fmt) + 
    required("-o", out) + 
    required(input_fastq)
  }
}
