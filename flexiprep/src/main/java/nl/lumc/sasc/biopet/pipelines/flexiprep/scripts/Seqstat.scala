package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import nl.lumc.sasc.biopet.wrappers.Python
import org.broadinstitute.sting.commandline._
import java.io.File

class Seqstat(val globalConfig: Config) extends CommandLineFunction with Python {
  def this() = this(new Config(Map()))
  analysisName = "seqstat"
  val config: Config = globalConfig.getAsConfig(analysisName)
  logger.debug("Config for " + analysisName + ": " + config)
  
  setPythonScript("__init__.py", "scripts/pyfastqc/")
  setPythonScript("seq_stat.py", "scripts/")
  
  @Input(doc="Fastq input", shortName="fastqc", required=true) var input_fastq: File = _
  @Input(doc="Dep", shortName="dep", required=false) var deps: List[File] = Nil
  @Output(doc="Output file", shortName="out", required=true) var out: File = _
  
  var fmt: String = _
  
  def commandLine = {
    getPythonCommand + 
    optional("--fmt", fmt) + 
    required("-o", out) + 
    required(input_fastq)
  }
}
