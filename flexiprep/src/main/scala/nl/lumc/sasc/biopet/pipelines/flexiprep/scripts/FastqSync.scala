package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

//import java.io.FileOutputStream
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.function._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class FastqSync(val globalConfig: Config) extends Python {
  def this() = this(new Config(Map()))
  analysisName = "fastqsync"
  val config = Config.mergeConfigs(globalConfig.getAsConfig(analysisName.toLowerCase), globalConfig)
  logger.debug("Config for " + analysisName + ": " + config)
  
  setPythonScript("__init__.py", "scripts/pyfastqc/")
  setPythonScript("sync_paired_end_reads.py", "scripts/")
  
  @Input(doc="Start fastq") var input_start_fastq: File = _
  @Input(doc="R1 input") var input_R1: File = _
  @Input(doc="R2 input") var input_R2: File = _
  @Output(doc="R1 output") var output_R1: File = _
  @Output(doc="R2 output") var output_R2: File = _
  var output_stats: File = _
  
  def commandLine = {
    getPythonCommand + 
    required(input_start_fastq) +
    required(input_R1) +
    required(input_R2) +
    required(output_R1) +
    required(output_R2) +
    " > " +
    required(output_stats)
  }
}