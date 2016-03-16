package nl.lumc.sasc.biopet.pipelines.gwastest

import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvanthof on 16/03/16.
  */
class GwasTest(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  /** Init for pipeline */
  def init(): Unit = {
  }

  /** Pipeline itself */
  def biopetScript(): Unit = {

  }
}
