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
package nl.lumc.sasc.biopet.extensions.bcftools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Output, Input}

class BcftoolsReheader(val parent: Configurable) extends Bcftools {

  @Input(doc = "Input vcf file", required = true)
  var input: File = _

  @Input(doc = "File specifying how sample names should be renamed", required = true)
  var renameSamples: File = _

  @Output(doc = "Output vcf file", required = true)
  var output: File = _

  def cmdLine = required(executable) +
      required("reheader") +
      required("--samples", renameSamples)
      required("--output", output)
      required(input)
}

object BcftoolsReheader {
  def apply(parent: Configurable, input: File, renameSamples: File, output: File): BcftoolsReheader = {
    val reheader = new BcftoolsReheader(parent)
    reheader.input = input
    reheader.renameSamples = renameSamples
    reheader.output = output
    reheader
  }
}
