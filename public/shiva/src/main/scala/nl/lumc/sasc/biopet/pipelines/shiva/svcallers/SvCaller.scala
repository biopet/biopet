package nl.lumc.sasc.biopet.pipelines.shiva.svcallers

import nl.lumc.sasc.biopet.core.{Reference, BiopetQScript}
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvanthof on 23/11/15.
  */
trait SvCaller extends QScript with BiopetQScript with Reference {

  /** Name of mode, this should also be used in the config */
  def name: String

  var namePrefix: String = _

  var inputBams: Map[String, File] = _

  def init() = {}
}






