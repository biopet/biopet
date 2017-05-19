package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

class MuTect2(val parent: Configurable) extends CommandLineGATK {

  def analysis_type: String = "MuTect2"

  /** Bam file for the tumor sample. */
  @Input(fullName = "tumor_bam", required = true)
  var tumorSampleBam: File = _

  /** Bam file for the normal sample. */
  @Input(fullName = "normal_bam", required = true)
  var normalSampleBam: File = _

  /** Vcf file of the dbSNP database. When it's provided, then it's possible to use the param 'dbsnpNormalLod', see the
    * description of that parameter for explanation. sIDs from this file are used to populate the ID column of the output.
    * Also, the DB INFO flag will be set when appropriate.
    * */
  @Input(fullName = "dbsnp", shortName = "D", required = false)
  var dbsnp: Option[File] = dbsnpVcfFile

  /** TODO  */
  @Input(fullName = "cosmic", shortName = "cosmic", doc = "", required = false)
  var cosmic: Option[File] = None

  /** Output file of the program. */
  @Output(fullName = "out", shortName = "o", required = true)
  var outputVcf: File = _

  /**
    * The very first threshold value that is used when assessing if a particular site is a variant in the tumor sample.
    * Two probabilities are calculated: probability of the model that the site is a variant and the probability of the
    * model that it's not. A site is classified as a candidate variant if the logarithm of the ratio between these 2
    * probabilities is higher than this threshold. Candidate variants are subject to further filtering but this value
    * decides if the site enters this processing, it is used for the initial selection. Raising the value increases
    * specificity and lowers sensitivity.
    *
    * Default value: 6.3
    * */
  @Argument(fullName = "tumor_lod", required = false)
  var tumorLOD: Option[Double] = None

  /** TODO: unclear how it differs from the param above 'tumorLod'.
    * Default value: 4.0
    * */
  @Argument(fullName = "initial_tumor_lod", required = false)
  var initialTumorLOD: Option[Double] = None

  /**
    * A threshold used when deciding if a variant found in tumor is reference in the normal sample (the parameter used when
    * finding variants in tumor is described above - 'tumorLod'). If it is reference in the normal, then the variant is
    * classified as a somatic variant existing only in tumor. The threshold is applied on the logarithm of the ratio
    * between the probabilities of the model that normal doesn't have a variant at the site and the probability
    * of the model that it has a variant.
    *
    * Default value: 2.2
    * */
  @Argument(fullName = "normal_lod", required = false)
  var normalLOD: Option[Double] = None

  /** TODO: unclear how it differs from the param above 'normalLod'.
    * Default value: 0.5
    * */
  @Argument(fullName = "initial_normal_lod", required = false)
  var initialNormalLOD: Option[Double] = None

  /**
    * Modelling takes into account also the probability for a site to have a variant in the general population. For sites
    * in the dbSNP database the probability is higher, so the threshold applied when deciding that a variant found in tumor
    * is missing in the normal should also be higher. If a site is in the dbSNP database then this threshold is used, if it's
    * not then the parameter 'normalLod' is used.
    *
    * Default value: 5.5
    * */
  @Argument(fullName = "dbsnp_normal_lod", required = false, otherArgumentRequired = "dbsnp")
  var dbsnpNormalLod: Option[Double] = None

  /** Ploidy per sample. For pooled data, this should be set to (Number of samples in each pool x Sample Ploidy).
    * Default value: 2
    * */
  @Argument(fullName = "sample_ploidy", shortName="ploidy", required = false)
  var ploidy: Option[Int] = None

  /*override def cmdLine = {
    super.input_file =
    super.cmdLine
  }*/
}

object MuTect2 {
  def apply(parent: Configurable, tumorSampleBam: File, normalSampleBam: File, output: File): MuTect2 = {
    val mutect2 = new MuTect2(parent)
    mutect2.tumorSampleBam = tumorSampleBam
    mutect2.normalSampleBam = normalSampleBam
    mutect2.outputVcf = output
    mutect2
  }
}
