package nl.lumc.sasc.biopet.pipelines.basty

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 3/4/15.
 */
class Basty(val root: Configurable) extends QScript with BastyTrait {
  def this() = this(null)
}

object Basty extends PipelineCommand