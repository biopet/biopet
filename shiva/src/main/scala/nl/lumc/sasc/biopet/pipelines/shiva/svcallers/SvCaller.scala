/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
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

  var inputBams: Map[String, File] = Map.empty

  val sampleNameSuffix: String = config("samplename_suffix", default = "")

  def outputVCF(sample: String): Option[File] = {
    outputVCFs.get(sample) match {
      case Some(file) => Some(file)
      case _ => None
    }
  }

  protected var outputVCFs: Map[String, File] = Map.empty

  protected def addVCF(sampleId: String, outputVCF: File) = {
    outputVCFs += (sampleId -> outputVCF)
  }

  def init() = {}
}
