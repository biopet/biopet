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

import nl.lumc.sasc.biopet.extensions.clever.{ CleverCaller, CleverFixVCF }
import nl.lumc.sasc.biopet.extensions.picard.SortVcf
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Script for sv caler Clever */
class Clever(val parent: Configurable) extends SvCaller {
  def name = "clever"

  def biopetScript() {
    for ((sample, bamFile) <- inputBams) {
      val cleverDir = new File(outputDir, sample)
      val clever = CleverCaller(this, bamFile, new File(cleverDir, "clever_output"))
      clever.jobOutputFile = new File(cleverDir, ".CleverCaller.out")
      add(clever)

      val cleverVCF = new CleverFixVCF(this)
      cleverVCF.input = clever.outputvcf
      cleverVCF.output = new File(cleverDir, s".${sample}.clever.vcf")
      cleverVCF.sampleName = sample + sampleNameSuffix
      cleverVCF.isIntermediate = true
      add(cleverVCF)

      val compressedVCF = new SortVcf(this)
      compressedVCF.input = cleverVCF.output
      compressedVCF.output = new File(cleverDir, s"${sample}.clever.vcf.gz")
      add(compressedVCF)

      addVCF(sample, compressedVCF.output)
    }
  }
}
