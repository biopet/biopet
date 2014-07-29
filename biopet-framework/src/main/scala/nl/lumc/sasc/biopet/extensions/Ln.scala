package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File
import scala.sys.process.Process

class Ln(val root: Configurable) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  @Input(doc = "Input file")
  var in: File = _

  @Output(doc = "Link destination")
  var out: File = _

  override def run {
    val cmd = "ln -s " + in + " " + out
    val process = Process(cmd).run
    System.out.println("cmd: '" + cmd + "', exitcode: " + process.exitValue)
  }
}
