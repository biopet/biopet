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
/**
  * Due to the license issue with GATK, this part of Biopet can only be used inside the
  * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
  * on how to use this protected part of biopet or contact us at sasc@lumc.nl
  */
package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import nl.lumc.sasc.biopet.extensions.gatk
import nl.lumc.sasc.biopet.extensions.gatk.gather.BqsrGather
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Default mode for the haplotypecaller */
class HaplotypeCaller(val parent: Configurable) extends Variantcaller {
  val name = "haplotypecaller"
  protected def defaultPrio = 1

  def biopetScript() {
    val hc = gatk.HaplotypeCaller(this, inputBams.values.toList, outputFile)
    hc.BQSR =
      if (inputBqsrFiles.isEmpty) None
      else {
        val gather = new BqsrGather
        gather.inputBqsrFiles = inputBqsrFiles.values.toList
        gather.outputBqsrFile = new File(outputDir, "bqsr.merge")
        add(gather)
        Some(gather.outputBqsrFile)
      }
    add(hc)
  }
}
