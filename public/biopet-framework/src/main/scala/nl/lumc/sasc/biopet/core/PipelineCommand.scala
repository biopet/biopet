package nl.lumc.sasc.biopet.core

import org.broadinstitute.gatk.queue.util.{ Logging => GatkLogging }
import java.io.File
import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.core.workaround.BiopetQCommandLine

trait PipelineCommand extends MainCommand with GatkLogging {

  def pipeline = "/" + getClass.getName.stripSuffix("$").replaceAll("\\.", "/") + ".class"

  def main(args: Array[String]): Unit = {
    val argsSize = args.size
    for (t <- 0 until argsSize) {
      if (args(t) == "-config" || args(t) == "--config_file") {
        if (t >= argsSize) throw new IllegalStateException("-config needs a value")
        Config.global.loadConfigFile(new File(args(t + 1)))
      }
    }

    var argv: Array[String] = Array()
    argv ++= Array("-S", pipeline)
    argv ++= args
    BiopetQCommandLine.main(argv)
  }
}