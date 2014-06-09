package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.InProcessFunction
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.sys.process._

class Ln(val globalConfig: Config) extends InProcessFunction {
  def this() = this(new Config(Map()))
  this.analysisName = "ln"
  val config: Config = Config.mergeConfigs(globalConfig.getAsConfig(analysisName), globalConfig)
  
  @Input(doc="Input file") var in: File = _
  @Output(doc="Link destination") var out: File = _
    
  override def run {
    val cmd = "ln -s " + in + " " + out
    val exitcode = cmd.!
    System.out.println("cmd: '" + cmd + "', exitcode: " + exitcode)
  }
}
