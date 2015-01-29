package nl.lumc.sasc.biopet.pipelines.bamtobigwig

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/29/15.
 */
class BamToBigWig(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  def init(): Unit = {
  }

  def biopetScript(): Unit = {
  }
}

object BamToBigWig extends PipelineCommand