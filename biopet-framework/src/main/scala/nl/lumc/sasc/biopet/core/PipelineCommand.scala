package nl.lumc.sasc.biopet.core

import org.broadinstitute.gatk.queue.util.Logging

trait PipelineCommand extends MainCommand with Logging {

  def pipeline = "/" + getClass.getName.stripSuffix("$").replaceAll("\\.", "/") + ".class"

  def main(args: Array[String]): Unit = {
    var argv: Array[String] = Array()
    argv ++= Array("-S", pipeline)
    argv ++= args
    BiopetQCommandLine.main(argv)
  }
}