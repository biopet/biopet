package nl.lumc.sasc.biopet.pipelines.shiva.svcallers

import nl.lumc.sasc.biopet.core.{ Reference, BiopetQScript }
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 23/11/15.
 */
trait SvCaller extends QScript with BiopetQScript with Reference {

  /** Name of mode, this should also be used in the config */
  def name: String

  var namePrefix: String = _

  var inputBams: Map[String, File] = Map.empty

  def outputVCF(sample: String): Option[File] = {
    outputVCFs.get(sample) match {
      case Some(file) => Some(file)
      case _          => None
    }
  }

  protected var outputVCFs: Map[String, File] = Map.empty

  protected def addVCF(sampleId: String, outputVCF: File) = {
    outputVCFs += (sampleId -> outputVCF)
  }

  def init() = {}
}
