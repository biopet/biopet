package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
//import org.broadinstitute.sting.queue.function.QFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class Ln(val globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  this.analysisName = "ln"
  val config: Config = globalConfig.getAsConfig(this.analysisName)
  logger.debug("Config for " + this.analysisName + ": " + config)
  
  @Input(doc="Input file") var in: File = _
  @Output(doc="Link destination") var out: File = _

  def commandLine = "ln -sf %s %s".format(in, out)
}

object Ln {
  def addLn(input:File, output:File) : Ln = {
    return new Ln { this.in = input; this.out = output }
  }
}