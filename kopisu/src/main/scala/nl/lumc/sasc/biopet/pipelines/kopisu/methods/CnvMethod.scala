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
package nl.lumc.sasc.biopet.pipelines.kopisu.methods

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.Reference
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 10/05/16.
 */
trait CnvMethod extends QScript with SummaryQScript with Reference {

  /** Name of mode, this should also be used in the config */
  def name: String

  var namePrefix: String = name

  var inputBams: Map[String, File] = Map.empty

  /** Name of summary output file */
  def summaryFile: File = new File(outputDir, s"$name.summary.json")

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = inputBams.map(x => s"inputbam_${x._1}" -> x._2) ++ cnvOutputFiles

  def init() = {}

  protected var cnvOutputFiles: Map[String, File] = Map.empty

  def getCnvOutputFiles: Map[String, File] = cnvOutputFiles

  protected def addOutput(sample: String, outputFile: File) = {
    cnvOutputFiles += (sample -> outputFile)
  }
}
