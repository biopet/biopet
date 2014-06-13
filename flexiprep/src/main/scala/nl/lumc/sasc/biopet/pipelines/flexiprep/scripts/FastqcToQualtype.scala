package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import nl.lumc.sasc.biopet.function.Python
import org.broadinstitute.sting.commandline._
import java.io.File

class FastqcToQualtype(val globalConfig: Config) extends CommandLineFunction with Python {
  def this() = this(new Config(Map()))
  analysisName = "getqualtype"
  val config: Config = Config.mergeConfigs(globalConfig.getAsConfig(analysisName), globalConfig)
  logger.debug("Config for " + analysisName + ": " + config)
  
  setPythonScript("__init__.py", "scripts/pyfastqc/")
  setPythonScript("qual_type_sickle.py", "scripts/")
  
  @Input(doc="Fastqc output", shortName="fastqc", required=true) var fastqc_output: File = _
  @Output(doc="Output file", shortName="out", required=true) var out: File = _
  
  def commandLine = {
    getPythonCommand +
    required(fastqc_output.getParent()) +
    " > " +
    required(out)
  }
}