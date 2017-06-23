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
package nl.lumc.sasc.biopet.pipelines.kopisu

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{PipelineCommand, Reference}
import nl.lumc.sasc.biopet.pipelines.kopisu.methods.{
  CnmopsMethod,
  ConiferMethod,
  FreecMethod,
  XhmmMethod
}
import nl.lumc.sasc.biopet.utils.{BamUtils, Logging}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.language.reflectiveCalls

class Kopisu(val parent: Configurable) extends QScript with SummaryQScript with Reference {
  qscript =>
  def this() = this(null)

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM", required = true)
  protected[kopisu] var inputBamsArg: List[File] = Nil

  var inputBams: Map[String, File] = Map()

  def init(): Unit = {
    if (inputBamsArg.nonEmpty) inputBams = BamUtils.sampleBamMap(inputBamsArg)
    if (inputBams.isEmpty) Logging.addError("No input bams found")
  }

  lazy val freecMethod = if (config("use_freec_method", default = true)) {
    Some(new FreecMethod(this))
  } else None

  lazy val coniferMethod = if (config("use_conifer_method", default = false)) {
    Some(new ConiferMethod(this))
  } else None

  lazy val cnMopsMethod = if (config("use_cnmops_method", default = false)) {
    Some(new CnmopsMethod(this))
  } else None

  lazy val xhmmMethod = if (config("use_xhmm_method", default = false)) {
    Some(new XhmmMethod(this))
  } else None

  // This script is in fact FreeC only.
  def biopetScript() {
    if (freecMethod.isEmpty && coniferMethod.isEmpty && cnMopsMethod.isEmpty && xhmmMethod.isEmpty)
      Logging.addError("No CNV method selected")

    freecMethod.foreach { method =>
      method.inputBams = inputBams
      method.outputDir = new File(outputDir, "freec_method")
      add(method)
    }

    coniferMethod.foreach { method =>
      method.inputBams = inputBams
      method.outputDir = new File(outputDir, "conifer_method")
      add(method)
    }

    cnMopsMethod.foreach { method =>
      method.inputBams = inputBams
      method.outputDir = new File(outputDir, "cnmops_method")
      add(method)
    }

    xhmmMethod.foreach { method =>
      method.inputBams = inputBams
      method.outputDir = new File(outputDir, "xhmm_method")
      add(method)
    }

    addSummaryJobs()
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map(
    "reference" -> referenceSummary,
    "freec_method" -> freecMethod.isDefined,
    "conifer_method" -> coniferMethod.isDefined,
    "cnmops_method" -> cnMopsMethod.isDefined,
    "xhmm_method" -> xhmmMethod.isDefined
  )

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = inputBams.map(x => s"inputbam_${x._1}" -> x._2)
}

object Kopisu extends PipelineCommand
