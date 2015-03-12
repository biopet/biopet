package nl.lumc.sasc.biopet.pipelines.shiva

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.PipelineCommand
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 2/24/15.
 */
class Shiva(val root: Configurable) extends QScript with ShivaTrait {
  def this() = this(null)
}

/** This object give a default main method to the pipelines */
object Shiva extends PipelineCommand