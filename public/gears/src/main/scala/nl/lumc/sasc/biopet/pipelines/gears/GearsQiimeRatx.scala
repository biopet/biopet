package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvan_thof on 12/4/15.
  */
class GearsQiimeRatx(val root: Configurable) extends QScript with BiopetQScript {

  var fastaR1: File = _

  var fastqR2: Option[File] = None

  def init() = {
    require(fastaR1 != null)
  }

  def biopetScript() = {

  }
}
