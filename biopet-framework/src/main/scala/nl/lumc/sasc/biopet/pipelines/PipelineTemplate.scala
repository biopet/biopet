package nl.lumc.sasc.biopet.pipelines.pipelinetemplate

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function._

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
