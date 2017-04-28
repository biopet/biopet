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

import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import nl.lumc.sasc.biopet.extensions.tools.{MpileupToVcf, VcfFilter}
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Makes a vcf file from a mpileup without statistics */
class RawVcf(val parent: Configurable) extends Variantcaller {
  val name = "raw"

  // This caller is designed as fallback when other variantcallers fails to report
  protected def defaultPrio = Int.MaxValue

  val keepRefCalls: Boolean = config("keep_ref_calls", default = false)

  def biopetScript {
    val rawFiles = inputBams.map {
      case (sample, bamFile) =>
        val mp = new SamtoolsMpileup(this) {
          override def configNamespace = "samtoolsmpileup"
          override def defaults =
            Map("samtoolsmpileup" -> Map("disable_baq" -> true, "min_map_quality" -> 1))
        }
        mp.input :+= bamFile

        val m2v = new MpileupToVcf(this)
        m2v.inputBam = bamFile
        m2v.output = new File(outputDir, sample + ".raw.vcf")
        val pipe = mp | m2v
        add(pipe)

        val vcfFilter = new VcfFilter(this) {
          override def configNamespace = "vcffilter"
          override def defaults =
            Map("min_sample_depth" -> 8,
                "min_alternate_depth" -> 2,
                "min_samples_pass" -> 1,
                "filter_ref_calls" -> !keepRefCalls)
        }
        vcfFilter.inputVcf = m2v.output
        vcfFilter.outputVcf =
          new File(outputDir, bamFile.getName.stripSuffix(".bam") + ".raw.filter.vcf.gz")
        add(vcfFilter)
        vcfFilter.outputVcf
    }

    val cv = new CombineVariants(this)
    cv.variant = rawFiles.toList
    cv.out = outputFile
    cv.setKey = Some("null")
    cv.excludeNonVariants = !keepRefCalls
    add(cv)
  }
}
