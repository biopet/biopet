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

  @Input(fullName = "normal_panel", shortName = "PON", doc = "", required = false)
  var ponFile: Option[File] = None

  @Input(fullName = "contamination_fraction_per_sample_file", shortName = "contaminationFile", doc = "", required = false)
  var contaminationFile: Option[File] = None

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
  var ploidy: Option[Int] = config("sample_ploidy")

  /** Default value: 0
    * */
  @Argument(fullName = "contamination_fraction_to_filter", shortName="contamination", required = false)
  var contaminationFractionToFilter: Option[Double] = config("contamination_fraction_to_filter")

  /** One or more classes/groups of annotations to apply to variant calls */
  @Argument(fullName = "group", shortName = "G", doc = "One or more classes/groups of annotations to apply to variant calls", required = false)
  var group: List[String] = config("group", default = Nil)

  /** Heterozygosity value used to compute prior likelihoods for any locus.
    * Default value:  0.001 */
  @Argument(fullName = "heterozygosity", shortName = "hets", doc = "Heterozygosity value used to compute prior likelihoods for any locus", required = false)
  var heterozygosity: Option[Double] = config("heterozygosity")

  /** Heterozygosity for indel calling.
    * Default value:  0.000125 */
  @Argument(fullName = "indel_heterozygosity", shortName = "indelHeterozygosity", doc = "Heterozygosity for indel calling", required = false)
  var heterozygosityForIndels: Option[Double] = config("indel_heterozygosity")

  /** Standard deviation of heterozygosity.
    * Default value:  0.01 */
  @Argument(fullName = "heterozygosity_stdev", shortName = "heterozygosityStandardDeviation", doc = "Standard deviation of heterozygosity", required = false)
  var heterozygositySD: Option[Double] = config("heterozygosity_stdev")

  /** Value used for filtering tumor variants. If a position that has been found to have a variant in tumor, has reads with the variant alternate allele also in the normal sample and if the number of such reads is higher than the value given with this parameter and the sum of the quality scores in these reads in this position is higher than given with the parameter 'maxAltAllelesInNormalQScoreSum', then the variant is filtered out from the final result.
    * Default value:  1 */
  @Argument(fullName = "max_alt_alleles_in_normal_count", required = false)
  var maxAltAllelesInNormalCount: Option[Int] = config("max_alt_alleles_in_normal_count")

  /** Value used for filtering tumor variants. If a position that has been found to have a variant in tumor, has reads with the variant alternate allele also in the normal sample and if the fraction of such reads from all reads is higher than the value given with this parameter and the sum of the quality scores in these reads in this position is higher than given with the parameter 'maxAltAllelesInNormalQScoreSum', then the variant is filtered out from the final result.
    * Default value:  0.03 */
  @Argument(fullName = "max_alt_alleles_in_normal_fraction", required = false)
  var maxAltAllelesInNormalFraction: Option[Double] = config("max_alt_alleles_in_normal_fraction")

  /** For explanation see the description of the parameters above.
    * Default value:  20 */
  @Argument(fullName = "max_alt_alleles_in_qscore_sum", required = false)
  var maxAltAllelesInNormalQScoreSum: Option[Int] = config("max_alt_alleles_in_qscore_sum")

  /** Minimum base quality required to consider a base for calling.
    * Default value:  10 */
  @Argument(fullName = "min_base_quality_score", shortName = "mbq", required = false)
  var minBaseQScore: Option[Int] = config("min_base_quality_score")

  /** Value used for filtering tumor variants to exclude false positives caused by misalignments evidenced by alternate alleles occurring near the start and end of reads. Variants are rejected if their median distance from the start/end of of the reads is lower than this parameter and if the median absolute deviation is lower than the value given with the parameter 'pir_mad_threshold'. Filtering is done only if the parameter 'enableClusteredReadPositionFilter' is set to true.
    * Default value:  10 */
  @Argument(fullName = "pir_median_threshold", required = false)
  var pirMedianThreshold: Option[Double] = config("pir_median_threshold")

  /** Value used for filtering tumor variants to exclude false positives caused by misalignments evidenced by alternate alleles occurring near the start and end of reads. Variants are rejected if their median distance from the start/end of the reads is lower than given with the parameter 'pirMedianThreshold' and if the median absolute deviation is lower than given with this parameter. Filtering is done only if the parameter 'enableClusteredReadPositionFilter' is set to true.
    * Default value:  3 */
  @Argument(fullName = "pir_mad_threshold", required = false)
  var pirMadThreshold: Option[Double] = config("pir_mad_threshold")

  /** If this parameter is set to true, then tumor variants are filtered to exclude false positives that are caused by misalignments evidenced by alternate alleles occurring near the start and end of reads. For filtering values of the parameters 'pirMedianThreshold' and 'pirMadThreshold' are used.
    * Default value:  false */
  @Argument(fullName = "enable_clustered_read_position_filter", required = false)
  var enableClusteredReadPositionFilter: Option[Boolean] = config("enable_clustered_read_position_filter")

  override def cmdLine = super.cmdLine +
    required("-I:tumor", tumorSampleBam) +
    required("-I:normal", normalSampleBam) +
    required("-o", outputVcf)
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
