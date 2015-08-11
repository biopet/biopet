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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions.samtools

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/** Extension for samtools flagstat */
class SamtoolsFaidx(val root: Configurable) extends Samtools {
  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "output File")
  private var _output: File = _

  def output = _output

  override def beforeGraph: Unit = {
    _output = new File(input.getParentFile, input.getName + ".fai")
  }

  /** Returns command to execute */
  def cmdLine = required(executable) + required("faidx") + required(input)
}

object SamtoolsFaidx {
  def apply(root: Configurable, input: File): SamtoolsFaidx = {
    val faidx = new SamtoolsFaidx(root)
    faidx.input = input
    faidx._output = new File(input.getParentFile, input.getName + ".fai")
    faidx
  }
}