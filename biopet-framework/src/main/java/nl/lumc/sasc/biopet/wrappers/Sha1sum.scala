package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class Sha1sum(val globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  this.analysisName = "sha1sum"
  val config: Config = Config.mergeConfigs(globalConfig.getAsConfig(analysisName), globalConfig)
  logger.debug("Config for " + this.analysisName + ": " + config)
  
  @Input(doc="Zipped file") var in: File = _
  @Output(doc="Unzipped file") var out: File = _
  
  def commandLine = "sha1sum %s > %s".format(in, out)
}