package nl.lumc.sasc.biopet.pipelines.gentrap

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk._
import org.broadinstitute.gatk.queue.extensions.picard._
import org.broadinstitute.gatk.queue.function._
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

class Gentrap(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  def init() {
  }

  def biopetScript() {
  }
}

object Gentrap extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gentrap/Gentrap.class"
}
