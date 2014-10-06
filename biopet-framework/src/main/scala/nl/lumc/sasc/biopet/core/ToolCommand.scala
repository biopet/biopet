package nl.lumc.sasc.biopet.core

import org.broadinstitute.gatk.queue.util.Logging


abstract trait ToolCommand extends Logging {

  lazy val toolName = this.getClass.getName
    .split("\\$").last.split("\\.").last

  def main(args: Array[String])
}
