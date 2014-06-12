package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import nl.lumc.sasc.biopet.function.Python
import org.broadinstitute.sting.commandline._
import java.io.File

class Summarize(val globalConfig: Config) extends CommandLineFunction with Python {
  def this() = this(new Config(Map()))
  analysisName = "flexiprep_sumarize"
  val config: Config = Config.mergeConfigs(globalConfig.getAsConfig(analysisName), globalConfig)
  logger.debug("Config for " + analysisName + ": " + config)
  
  setPythonScript("__init__.py", "scripts/pyfastqc/")
  setPythonScript("summarize_flexiprep.py", "scripts/")
  
  @Input(doc="Dep", shortName="dep", required=false) var deps: List[File] = Nil
  @Output(doc="Output file", shortName="out", required=true) var out: File = _
  
  var samplea: String = _
  var sampleb: String = _
  var runDir: String = _
  var samplename: String = _
  var trim: Boolean = true
  var clip: Boolean = true
  
  def commandLine = {
    var mode: String = ""
    if (clip) mode += "clip"
    if (trim) mode += "trim"
    if (mode.isEmpty) mode = "none"
    
    getPythonCommand + 
    optional("--run-dir", runDir) +
    optional("--sampleb", sampleb) +
    required(samplename) + 
    required(mode) + 
    required(samplea) + 
    required(out)
  }
}