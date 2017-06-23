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

import nl.lumc.sasc.biopet.extensions.breakdancer.{
  BreakdancerCaller,
  BreakdancerConfig,
  BreakdancerVCF
}
import nl.lumc.sasc.biopet.extensions.picard.SortVcf
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Script for sv caler Breakdancer */
class Breakdancer(val parent: Configurable) extends SvCaller {
  def name = "breakdancer"

  def biopetScript() {
    for ((sample, bamFile) <- inputBams) {
      val breakdancerSampleDir = new File(outputDir, sample)

      // read config and set all parameters for the pipeline
      logger.debug("Starting Breakdancer configuration")

      val bdcfg = BreakdancerConfig(this,
                                    bamFile,
                                    new File(breakdancerSampleDir, sample + ".breakdancer.cfg"))
      val breakdancer = BreakdancerCaller(
        this,
        bdcfg.output,
        new File(breakdancerSampleDir, sample + ".breakdancer.tsv"))
      breakdancer.deps :+= bamFile
      val bdvcf = BreakdancerVCF(this,
                                 breakdancer.output,
                                 new File(breakdancerSampleDir, sample + ".breakdancer.vcf"),
                                 sample = sample + sampleNameSuffix)

      val compressedVCF = new SortVcf(this)
      compressedVCF.input = bdvcf.output
      compressedVCF.output = new File(breakdancerSampleDir, s"${sample}.breakdancer.vcf.gz")

      add(bdcfg, breakdancer, bdvcf, compressedVCF)

      addVCF(sample, compressedVCF.output)
    }
  }
}
