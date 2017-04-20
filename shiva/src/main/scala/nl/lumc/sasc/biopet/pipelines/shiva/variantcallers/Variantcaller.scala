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
package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import nl.lumc.sasc.biopet.core.MultiSampleQScript.Gender
import nl.lumc.sasc.biopet.core.{ BiopetQScript, Reference }
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 11/19/15.
 */
trait Variantcaller extends QScript with BiopetQScript with Reference {

  /** Name of mode, this should also be used in the config */
  def name: String

  var namePrefix: String = _

  var genders: Map[String, Gender.Value] = _

  val mergeVcfResults: Boolean = config("merge_vcf_results", default = true)

  /**
   * Map of samplename -> (preprocessed) bam file
   */
  var inputBams: Map[String, File] = _
  var inputBqsrFiles: Map[String, File] = Map()

  def init() = {}

  /** Prio in merging  in the final file */
  protected def defaultPrio: Int

  /** Prio from the config */
  lazy val prio: Int = config("prio_" + name, default = defaultPrio)

  /** Final output file of this mode */
  def outputFile: File = new File(outputDir, namePrefix + s".$name.vcf.gz")
}

