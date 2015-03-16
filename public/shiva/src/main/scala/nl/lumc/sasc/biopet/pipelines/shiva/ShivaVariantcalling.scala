package nl.lumc.sasc.biopet.pipelines.shiva

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 2/26/15.
 */
class ShivaVariantcalling(val root: Configurable) extends QScript with ShivaVariantcallingTrait {
  def this() = this(null)
}

object ShivaVariantcalling extends PipelineCommand