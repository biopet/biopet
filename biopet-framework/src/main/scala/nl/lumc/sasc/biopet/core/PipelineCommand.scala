package nl.lumc.sasc.biopet.core

import org.broadinstitute.gatk.queue.util.Logging

trait PipelineCommand extends Logging {
  val pipeline = ""

  def main(args: Array[String]): Unit = {
    var argv: Array[String] = Array()
    argv ++= Array("-S", pipeline)
    argv ++= args
    BiopetQCommandLine.main(argv)
  }
}