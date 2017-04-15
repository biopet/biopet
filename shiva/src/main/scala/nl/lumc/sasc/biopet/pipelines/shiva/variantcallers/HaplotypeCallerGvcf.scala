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

import nl.lumc.sasc.biopet.core.MultiSampleQScript.Gender
import nl.lumc.sasc.biopet.extensions.gatk
import nl.lumc.sasc.biopet.extensions.gatk.CombineGVCFs
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Gvcf mode for haplotypecaller */
class HaplotypeCallerGvcf(val parent: Configurable) extends Variantcaller {
  val name = "haplotypecaller_gvcf"
  protected def defaultPrio = 5

  /**
   * Map of sample name -> gvcf. May be empty.
   */
  protected var gVcfFiles: Map[String, File] = Map()

  def getGvcfs = gVcfFiles

  val genderAwareCalling: Boolean = config("gender_aware_calling", default = false)
  val haploidRegions: Option[File] = config("hap̦loid_regions")
  val haploidRegionsMale: Option[File] = config("haploid_regions")
  val haploidRegionsFemale: Option[File] = config("haploid_regions")

  override def fixedValues = Map("haplotypecaller" -> Map("emitRefConfidence" -> "GVCF"))

  def biopetScript() {
    gVcfFiles = for ((sample, inputBam) <- inputBams) yield {
      if (genderAwareCalling) {
        val finalFile = new File(outputDir, sample + ".gvcf.vcf.gz")
        val haploidBedFiles: List[File] = genders.getOrElse(sample, Gender.Unknown) match {
          case Gender.Female => haploidRegions.toList ::: haploidRegionsFemale.toList ::: Nil
          case Gender.Male   => haploidRegions.toList ::: haploidRegionsMale.toList ::: Nil
          case _             => haploidRegions.toList
        }

        val haploidGvcf = if (haploidBedFiles.nonEmpty) {
          val hc = gatk.HaplotypeCaller(this, List(inputBam), new File(outputDir, sample + ".haploid.gvcf.vcf.gz"))
          hc.BQSR = inputBqsrFiles.get(sample)
          hc.intervals = haploidBedFiles
          add(hc)
          Some(hc.out)
        } else None

        val hcDiploid = gatk.HaplotypeCaller(this, List(inputBam), new File(outputDir, sample + ".diploid.gvcf.vcf.gz"))
        hcDiploid.BQSR = inputBqsrFiles.get(sample)
        hcDiploid.excludeIntervals = haploidBedFiles
        add(hcDiploid)

        haploidGvcf match {
          case Some(file) =>
            val combine = new CombineGVCFs(this)
            combine.variant = Seq(hcDiploid.out, file)
            combine.out = new File(outputDir, sample + ".gvcf.vcf.gz")
            add(combine)
            sample -> combine.out
          case _ => sample -> hcDiploid.out
        }
      } else {
        val hc = gatk.HaplotypeCaller(this, List(inputBam), new File(outputDir, sample + ".gvcf.vcf.gz"))
        hc.BQSR = inputBqsrFiles.get(sample)
        add(hc)
        sample -> hc.out
      }
    }

    val genotypeGVCFs = gatk.GenotypeGVCFs(this, gVcfFiles.values.toList, outputFile)
    add(genotypeGVCFs)
  }
}
