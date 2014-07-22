package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.function.PythonCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{Input, Output}
import java.io.File

class Seqstat(val root:Configurable) extends PythonCommandLineFunction {
  setPythonScript("__init__.py", "pyfastqc/")
  setPythonScript("seq_stat.py")
  
  @Input(doc="Fastq input", shortName="fastqc", required=true)
  var input_fastq: File = _
  
  @Output(doc="Output file", shortName="out", required=true)
  var out: File = _
  
  var fmt: String = _
  
  def cmdLine = {
    getPythonCommand + 
    optional("--fmt", fmt) + 
    required("-o", out) + 
    required(input_fastq)
  }
}
