package nl.lumc.sasc.biopet.pipelines.pipelinetemplate

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.extensions._
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function._
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

class PipelineTemplate(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
  }

  def biopetScript() {
  }
}

object PipelineTemplate extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/pipelinetemplate/PipelineTemplate.class"
}
