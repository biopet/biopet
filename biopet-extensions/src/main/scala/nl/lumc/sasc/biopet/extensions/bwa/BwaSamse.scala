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
package nl.lumc.sasc.biopet.extensions.bwa

import java.io.File

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * BWA samse wrapper
 *
 * based on executable version 0.7.10-r789
 *
 */
class BwaSamse(val root: Configurable) extends Bwa with Reference {
  @Input(doc = "Fastq file", required = true)
  var fastq: File = _

  @Input(doc = "Sai file", required = true)
  var sai: File = _

  @Input(doc = "The reference file for the bam files.", required = true)
  var reference: File = null

  @Output(doc = "Output file SAM", required = false)
  var output: File = _

  var n: Option[Int] = config("n")
  var r: String = _

  override def beforeGraph() {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
  }

  /** Returns command to execute */
  def cmdLine = required(executable) +
    required("samse") +
    optional("-n", n) +
    optional("-f", output) +
    optional("-r", r) +
    required(reference) +
    required(sai) +
    required(fastq)
}
