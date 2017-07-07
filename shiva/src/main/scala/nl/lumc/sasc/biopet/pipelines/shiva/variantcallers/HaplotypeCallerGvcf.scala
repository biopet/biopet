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

import nl.lumc.sasc.biopet.core.MultiSampleQScript.Gender
import nl.lumc.sasc.biopet.extensions.gatk
import nl.lumc.sasc.biopet.pipelines.shiva.GenotypeGvcfs
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList

/** Gvcf mode for haplotypecaller */
class HaplotypeCallerGvcf(val parent: Configurable) extends Variantcaller {
  val name = "haplotypecaller_gvcf"
  protected def defaultPrio = 5

  /** Map of sample name -> gvcf. May be empty. */
  protected var gVcfFiles: Map[String, File] = Map()

  def getGvcfs: Map[String, File] = gVcfFiles

  val genderAwareCalling: Boolean = config("gender_aware_calling", default = false)
  val haploidRegions: Option[File] = config("hap̦loid_regions")
  val haploidRegionsMale: Option[File] = config("haploid_regions_male")
  val haploidRegionsFemale: Option[File] = config("haploid_regions_female")

  lazy val fractionMale: Double = BedRecordList
    .fromFiles(Seq(haploidRegions, haploidRegionsMale).flatten, combine = true)
    .fractionOfReference(referenceDict)
  lazy val fractionFemale: Double = BedRecordList
    .fromFiles(Seq(haploidRegions, haploidRegionsFemale).flatten, combine = true)
    .fractionOfReference(referenceDict)
  lazy val fractionUnknown: Double =
    BedRecordList
      .fromFiles(Seq(haploidRegions).flatten, combine = true)
      .fractionOfReference(referenceDict)

  override def fixedValues = Map("haplotypecaller" -> Map("emitRefConfidence" -> "GVCF"))

  override def init(): Unit = {
    super.init()
    if (genderAwareCalling && haploidRegions.isEmpty && haploidRegionsMale.isEmpty && haploidRegionsFemale.isEmpty)
      Logging.addError("Gender aware variantcalling is enabled but no haploid bed files are given")
  }

  protected val genotypeGvcfs = new GenotypeGvcfs(this)

  override def outputFile: File = genotypeGvcfs.finalVcfFile

  def finalGvcfFile: File = genotypeGvcfs.finalGvcfFile

  def biopetScript() {
    gVcfFiles = for ((sample, inputBam) <- inputBams) yield {
      if (genderAwareCalling) {
        val gender = genders.getOrElse(sample, Gender.Unknown)
        val haploidBedFiles: List[File] = gender match {
          case Gender.Female => haploidRegions.toList ::: haploidRegionsFemale.toList ::: Nil
          case Gender.Male => haploidRegions.toList ::: haploidRegionsMale.toList ::: Nil
          case _ => haploidRegions.toList
        }

        val fraction: Double = gender match {
          case Gender.Female => fractionFemale
          case Gender.Male => fractionMale
          case _ => fractionUnknown
        }

        val haploidGvcf = if (haploidBedFiles.nonEmpty) {
          val hc = gatk.HaplotypeCaller(this,
                                        List(inputBam),
                                        new File(outputDir, sample + ".haploid.g.vcf.gz"))
          hc.BQSR = inputBqsrFiles.get(sample)
          hc.intervals = haploidBedFiles
          hc.scatterCount = (hc.scatterCount * fraction).toInt
          hc.sample_ploidy = Some(1)
          add(hc)
          genotypeGvcfs.inputGvcfs :+= hc.out
          Some(hc.out)
        } else None

        val hcDiploid = gatk.HaplotypeCaller(this,
                                             List(inputBam),
                                             new File(outputDir, sample + ".diploid.g.vcf.gz"))
        hcDiploid.BQSR = inputBqsrFiles.get(sample)
        hcDiploid.excludeIntervals = haploidBedFiles
        hcDiploid.scatterCount = (hcDiploid.scatterCount * (1 - fraction)).toInt
        add(hcDiploid)
        genotypeGvcfs.inputGvcfs :+= hcDiploid.out

        haploidGvcf match {
          case Some(file) =>
            val combine = new gatk.CombineGVCFs(this)
            combine.variant = Seq(hcDiploid.out, file)
            combine.out = new File(outputDir, sample + ".g.vcf.gz")
            add(combine)
            sample -> combine.out
          case _ => sample -> hcDiploid.out
        }
      } else {
        val hc =
          gatk.HaplotypeCaller(this, List(inputBam), new File(outputDir, sample + ".g.vcf.gz"))
        hc.BQSR = inputBqsrFiles.get(sample)
        add(hc)
        genotypeGvcfs.inputGvcfs :+= hc.out
        sample -> hc.out
      }
    }

    genotypeGvcfs.outputDir = outputDir
    genotypeGvcfs.namePrefix = s"$namePrefix.$name"
    add(genotypeGvcfs)
  }
}
