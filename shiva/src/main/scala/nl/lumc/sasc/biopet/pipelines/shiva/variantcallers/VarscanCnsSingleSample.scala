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

import java.io.PrintWriter

import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import nl.lumc.sasc.biopet.extensions.varscan.{ FixMpileup, VarscanMpileup2cns }
import nl.lumc.sasc.biopet.extensions.{ Bgzip, Tabix }
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by sajvanderzeeuw on 15-1-16.
 */
class VarscanCnsSingleSample(val root: Configurable) extends Variantcaller {
  val name = "varscan_cns_singlesample"
  protected def defaultPrio = 25

  override def defaults = Map(
    "samtoolsmpileup" -> Map(
      "disable_baq" -> true,
      "depth" -> 1000000
    ),
    "varscanmpileup2cns" -> Map("strand_filter" -> 0),
    "combinevariants" -> Map("scattercount" -> 20)
  )

  override def fixedValues = Map(
    "samtoolsmpileup" -> Map("output_mapping_quality" -> true),
    "varscanmpileup2cns" -> Map("output_vcf" -> 1)
  )

  def biopetScript: Unit = {
    val sampleVcfs = for ((sample, inputBam) <- inputBams.toList) yield {
      val mpileup = new SamtoolsMpileup(this)
      mpileup.input = List(inputBam)

      val sampleVcf = new File(outputDir, s"${name}_$sample.vcf.gz")

      val sampleFile = new File(outputDir, s"$sample.name.txt")
      sampleFile.getParentFile.mkdirs()
      sampleFile.deleteOnExit()
      val writer = new PrintWriter(sampleFile)
      writer.println(sample)
      writer.close()

      val varscan = new VarscanMpileup2cns(this)
      varscan.vcfSampleList = Some(sampleFile)

      val variantcallingJob = mpileup | new FixMpileup(this) | varscan | new Bgzip(this) > sampleVcf
      variantcallingJob.threadsCorrection = -2
      variantcallingJob.mainFunction = true
      add(variantcallingJob)
      add(Tabix(this, sampleVcf))

      sampleVcf
    }

    val cv = new CombineVariants(this)
    cv.variant = sampleVcfs
    cv.out = outputFile
    cv.setKey = Some("null")
    cv.excludeNonVariants = true
    cv.mainFunction = true
    add(cv)
  }
}
