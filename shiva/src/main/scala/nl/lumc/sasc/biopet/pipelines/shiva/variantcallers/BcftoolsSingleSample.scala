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

import nl.lumc.sasc.biopet.extensions.{Ln, Tabix}
import nl.lumc.sasc.biopet.extensions.bcftools.{BcftoolsMerge, BcftoolsCall}
import nl.lumc.sasc.biopet.extensions.samtools.{FixMpileup, SamtoolsMpileup}
import nl.lumc.sasc.biopet.utils.config.Configurable

/** default mode of bcftools */
class BcftoolsSingleSample(val parent: Configurable) extends Variantcaller {
  val name = "bcftools_singlesample"
  protected def defaultPrio = 8

  def biopetScript() {
    val sampleVcfs = for ((sample, inputBam) <- inputBams.toList) yield {
      val mp = new SamtoolsMpileup(this)
      mp.input :+= inputBam
      mp.u = true
      mp.v = true
      mp.reference = referenceFasta()

      val bt = new BcftoolsCall(this)
      bt.O = Some("z")
      bt.v = true
      bt.c = true
      bt.output = new File(outputDir, sample + ".vcf.gz")

      val pipe = mp | new FixMpileup(this) | bt
      add(pipe)
      add(Tabix(this, bt.output))
      bt.output
    }

    if (sampleVcfs.size > 1) {
      val bcfmerge = new BcftoolsMerge(this)
      bcfmerge.input = sampleVcfs
      bcfmerge.output = outputFile
      bcfmerge.O = Some("z")
      add(bcfmerge)
    } else add(Ln.apply(this, sampleVcfs.head, outputFile))
    add(Tabix(this, outputFile))
  }
}
