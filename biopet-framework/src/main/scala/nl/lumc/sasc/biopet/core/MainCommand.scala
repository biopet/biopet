package nl.lumc.sasc.biopet.core

import org.broadinstitute.gatk.queue.util.Logging


trait MainCommand extends Logging {

  lazy val name = this.getClass.getSimpleName
    .split("\\$").last

  def main(args: Array[String])
}
