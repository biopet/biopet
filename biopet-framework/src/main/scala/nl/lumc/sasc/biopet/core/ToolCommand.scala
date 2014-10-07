package nl.lumc.sasc.biopet.core

import org.broadinstitute.gatk.queue.util.Logging


trait ToolCommand extends Logging {

  lazy val toolName = this.getClass.getSimpleName
    .split("\\$").last

  def main(args: Array[String])
}
