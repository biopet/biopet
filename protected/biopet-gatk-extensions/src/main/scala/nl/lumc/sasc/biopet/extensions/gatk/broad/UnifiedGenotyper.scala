/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

//import java.io.File
//
//import nl.lumc.sasc.biopet.utils.config.Configurable
//import org.broadinstitute.gatk.utils.commandline.{ Gather, Output }
//
//class UnifiedGenotyper(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.UnifiedGenotyper with GatkGeneral {
//
//  @Gather(enabled = false)
//  @Output(required = false)
//  protected var vcfIndex: File = _
//
//  if (config.contains("scattercount")) scatterCount = config("scattercount")
//  if (config.contains("dbsnp")) this.dbsnp = config("dbsnp")
//  sample_ploidy = config("ploidy")
//  if (config.contains("allSitePLs")) this.allSitePLs = config("allSitePLs")
//
//  stand_call_conf = config("stand_call_conf", default = 5)
//  stand_emit_conf = config("stand_emit_conf", default = 0)
//
//  if (config.contains("output_mode")) {
//    import org.broadinstitute.gatk.tools.walkers.genotyper.OutputMode._
//    config("output_mode").asString match {
//      case "EMIT_ALL_CONFIDENT_SITES" => output_mode = EMIT_ALL_CONFIDENT_SITES
//      case "EMIT_ALL_SITES"           => output_mode = EMIT_ALL_SITES
//      case "EMIT_VARIANTS_ONLY"       => output_mode = EMIT_VARIANTS_ONLY
//      case e                          => logger.warn("output mode '" + e + "' does not exist")
//    }
//  }
//
//  override val defaultThreads = 1
//
//  override def freezeFieldValues() {
//    super.freezeFieldValues()
//
//    genotype_likelihoods_model = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypeLikelihoodsCalculationModel.Model.BOTH
//    nct = Some(getThreads)
//    memoryLimit = Option(nct.getOrElse(1) * memoryLimit.getOrElse(2.0))
//  }
//}

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ CatVariantsGatherer, GATKScatterFunction, LocusScatterFunction, TaggedFile }
import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline.{ Gather, Input, Output, _ }

class UnifiedGenotyper(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  analysisName = "UnifiedGenotyper"
  analysis_type = "UnifiedGenotyper"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** Genotype likelihoods calculation model to employ -- SNP is the default option, while INDEL is also available for calling indels and BOTH is available for calling both together */
  @Argument(fullName = "genotype_likelihoods_model", shortName = "glm", doc = "Genotype likelihoods calculation model to employ -- SNP is the default option, while INDEL is also available for calling indels and BOTH is available for calling both together", required = false, exclusiveOf = "", validation = "")
  var genotype_likelihoods_model: String = _

  /**
   * Short name of genotype_likelihoods_model
   * @return Short name of genotype_likelihoods_model
   */
  def glm = this.genotype_likelihoods_model

  /**
   * Short name of genotype_likelihoods_model
   * @param value Short name of genotype_likelihoods_model
   */
  def glm_=(value: String) { this.genotype_likelihoods_model = value }

  /** The PCR error rate to be used for computing fragment-based likelihoods */
  @Argument(fullName = "pcr_error_rate", shortName = "pcr_error", doc = "The PCR error rate to be used for computing fragment-based likelihoods", required = false, exclusiveOf = "", validation = "")
  var pcr_error_rate: Option[Double] = None

  /**
   * Short name of pcr_error_rate
   * @return Short name of pcr_error_rate
   */
  def pcr_error = this.pcr_error_rate

  /**
   * Short name of pcr_error_rate
   * @param value Short name of pcr_error_rate
   */
  def pcr_error_=(value: Option[Double]) { this.pcr_error_rate = value }

  /** Format string for pcr_error_rate */
  @Argument(fullName = "pcr_error_rateFormat", shortName = "", doc = "Format string for pcr_error_rate", required = false, exclusiveOf = "", validation = "")
  var pcr_error_rateFormat: String = "%s"

  /** If provided, we will calculate the SLOD (SB annotation) */
  @Argument(fullName = "computeSLOD", shortName = "slod", doc = "If provided, we will calculate the SLOD (SB annotation)", required = false, exclusiveOf = "", validation = "")
  var computeSLOD: Boolean = _

  /**
   * Short name of computeSLOD
   * @return Short name of computeSLOD
   */
  def slod = this.computeSLOD

  /**
   * Short name of computeSLOD
   * @param value Short name of computeSLOD
   */
  def slod_=(value: Boolean) { this.computeSLOD = value }

  /** The PairHMM implementation to use for -glm INDEL genotype likelihood calculations */
  @Argument(fullName = "pair_hmm_implementation", shortName = "pairHMM", doc = "The PairHMM implementation to use for -glm INDEL genotype likelihood calculations", required = false, exclusiveOf = "", validation = "")
  var pair_hmm_implementation: org.broadinstitute.gatk.utils.pairhmm.PairHMM.HMM_IMPLEMENTATION = _

  /**
   * Short name of pair_hmm_implementation
   * @return Short name of pair_hmm_implementation
   */
  def pairHMM = this.pair_hmm_implementation

  /**
   * Short name of pair_hmm_implementation
   * @param value Short name of pair_hmm_implementation
   */
  def pairHMM_=(value: org.broadinstitute.gatk.utils.pairhmm.PairHMM.HMM_IMPLEMENTATION) { this.pair_hmm_implementation = value }

  /** Minimum base quality required to consider a base for calling */
  @Argument(fullName = "min_base_quality_score", shortName = "mbq", doc = "Minimum base quality required to consider a base for calling", required = false, exclusiveOf = "", validation = "")
  var min_base_quality_score: Option[Int] = None

  /**
   * Short name of min_base_quality_score
   * @return Short name of min_base_quality_score
   */
  def mbq = this.min_base_quality_score

  /**
   * Short name of min_base_quality_score
   * @param value Short name of min_base_quality_score
   */
  def mbq_=(value: Option[Int]) { this.min_base_quality_score = value }

  /** Maximum fraction of reads with deletions spanning this locus for it to be callable */
  @Argument(fullName = "max_deletion_fraction", shortName = "deletions", doc = "Maximum fraction of reads with deletions spanning this locus for it to be callable", required = false, exclusiveOf = "", validation = "")
  var max_deletion_fraction: Option[Double] = None

  /**
   * Short name of max_deletion_fraction
   * @return Short name of max_deletion_fraction
   */
  def deletions = this.max_deletion_fraction

  /**
   * Short name of max_deletion_fraction
   * @param value Short name of max_deletion_fraction
   */
  def deletions_=(value: Option[Double]) { this.max_deletion_fraction = value }

  /** Format string for max_deletion_fraction */
  @Argument(fullName = "max_deletion_fractionFormat", shortName = "", doc = "Format string for max_deletion_fraction", required = false, exclusiveOf = "", validation = "")
  var max_deletion_fractionFormat: String = "%s"

  /** Minimum number of consensus indels required to trigger genotyping run */
  @Argument(fullName = "min_indel_count_for_genotyping", shortName = "minIndelCnt", doc = "Minimum number of consensus indels required to trigger genotyping run", required = false, exclusiveOf = "", validation = "")
  var min_indel_count_for_genotyping: Option[Int] = None

  /**
   * Short name of min_indel_count_for_genotyping
   * @return Short name of min_indel_count_for_genotyping
   */
  def minIndelCnt = this.min_indel_count_for_genotyping

  /**
   * Short name of min_indel_count_for_genotyping
   * @param value Short name of min_indel_count_for_genotyping
   */
  def minIndelCnt_=(value: Option[Int]) { this.min_indel_count_for_genotyping = value }

  /** Minimum fraction of all reads at a locus that must contain an indel (of any allele) for that sample to contribute to the indel count for alleles */
  @Argument(fullName = "min_indel_fraction_per_sample", shortName = "minIndelFrac", doc = "Minimum fraction of all reads at a locus that must contain an indel (of any allele) for that sample to contribute to the indel count for alleles", required = false, exclusiveOf = "", validation = "")
  var min_indel_fraction_per_sample: Option[Double] = None

  /**
   * Short name of min_indel_fraction_per_sample
   * @return Short name of min_indel_fraction_per_sample
   */
  def minIndelFrac = this.min_indel_fraction_per_sample

  /**
   * Short name of min_indel_fraction_per_sample
   * @param value Short name of min_indel_fraction_per_sample
   */
  def minIndelFrac_=(value: Option[Double]) { this.min_indel_fraction_per_sample = value }

  /** Format string for min_indel_fraction_per_sample */
  @Argument(fullName = "min_indel_fraction_per_sampleFormat", shortName = "", doc = "Format string for min_indel_fraction_per_sample", required = false, exclusiveOf = "", validation = "")
  var min_indel_fraction_per_sampleFormat: String = "%s"

  /** Indel gap continuation penalty, as Phred-scaled probability.  I.e., 30 => 10 -30/10 */
  @Argument(fullName = "indelGapContinuationPenalty", shortName = "indelGCP", doc = "Indel gap continuation penalty, as Phred-scaled probability.  I.e., 30 => 10^-30/10", required = false, exclusiveOf = "", validation = "")
  var indelGapContinuationPenalty: Option[Byte] = None

  /**
   * Short name of indelGapContinuationPenalty
   * @return Short name of indelGapContinuationPenalty
   */
  def indelGCP = this.indelGapContinuationPenalty

  /**
   * Short name of indelGapContinuationPenalty
   * @param value Short name of indelGapContinuationPenalty
   */
  def indelGCP_=(value: Option[Byte]) { this.indelGapContinuationPenalty = value }

  /** Indel gap open penalty, as Phred-scaled probability.  I.e., 30 => 10 -30/10 */
  @Argument(fullName = "indelGapOpenPenalty", shortName = "indelGOP", doc = "Indel gap open penalty, as Phred-scaled probability.  I.e., 30 => 10^-30/10", required = false, exclusiveOf = "", validation = "")
  var indelGapOpenPenalty: Option[Byte] = None

  /**
   * Short name of indelGapOpenPenalty
   * @return Short name of indelGapOpenPenalty
   */
  def indelGOP = this.indelGapOpenPenalty

  /**
   * Short name of indelGapOpenPenalty
   * @param value Short name of indelGapOpenPenalty
   */
  def indelGOP_=(value: Option[Byte]) { this.indelGapOpenPenalty = value }

  /** Indel haplotype size */
  @Argument(fullName = "indelHaplotypeSize", shortName = "indelHSize", doc = "Indel haplotype size", required = false, exclusiveOf = "", validation = "")
  var indelHaplotypeSize: Option[Int] = None

  /**
   * Short name of indelHaplotypeSize
   * @return Short name of indelHaplotypeSize
   */
  def indelHSize = this.indelHaplotypeSize

  /**
   * Short name of indelHaplotypeSize
   * @param value Short name of indelHaplotypeSize
   */
  def indelHSize_=(value: Option[Int]) { this.indelHaplotypeSize = value }

  /** Output indel debug info */
  @Argument(fullName = "indelDebug", shortName = "indelDebug", doc = "Output indel debug info", required = false, exclusiveOf = "", validation = "")
  var indelDebug: Boolean = _

  /** expt */
  @Argument(fullName = "ignoreSNPAlleles", shortName = "ignoreSNPAlleles", doc = "expt", required = false, exclusiveOf = "", validation = "")
  var ignoreSNPAlleles: Boolean = _

  /** expt */
  @Argument(fullName = "allReadsSP", shortName = "dl", doc = "expt", required = false, exclusiveOf = "", validation = "")
  var allReadsSP: Boolean = _

  /**
   * Short name of allReadsSP
   * @return Short name of allReadsSP
   */
  def dl = this.allReadsSP

  /**
   * Short name of allReadsSP
   * @param value Short name of allReadsSP
   */
  def dl_=(value: Boolean) { this.allReadsSP = value }

  /** Ignore lane when building error model, error model is then per-site */
  @Argument(fullName = "ignoreLaneInfo", shortName = "ignoreLane", doc = "Ignore lane when building error model, error model is then per-site", required = false, exclusiveOf = "", validation = "")
  var ignoreLaneInfo: Boolean = _

  /**
   * Short name of ignoreLaneInfo
   * @return Short name of ignoreLaneInfo
   */
  def ignoreLane = this.ignoreLaneInfo

  /**
   * Short name of ignoreLaneInfo
   * @param value Short name of ignoreLaneInfo
   */
  def ignoreLane_=(value: Boolean) { this.ignoreLaneInfo = value }

  /** VCF file with the truth callset for the reference sample */
  @Input(fullName = "reference_sample_calls", shortName = "referenceCalls", doc = "VCF file with the truth callset for the reference sample", required = false, exclusiveOf = "", validation = "")
  var reference_sample_calls: File = _

  /**
   * Short name of reference_sample_calls
   * @return Short name of reference_sample_calls
   */
  def referenceCalls = this.reference_sample_calls

  /**
   * Short name of reference_sample_calls
   * @param value Short name of reference_sample_calls
   */
  def referenceCalls_=(value: File) { this.reference_sample_calls = value }

  /** Dependencies on the index of reference_sample_calls */
  @Input(fullName = "reference_sample_callsIndex", shortName = "", doc = "Dependencies on the index of reference_sample_calls", required = false, exclusiveOf = "", validation = "")
  private var reference_sample_callsIndex: Seq[File] = Nil

  /** Reference sample name. */
  @Argument(fullName = "reference_sample_name", shortName = "refsample", doc = "Reference sample name.", required = false, exclusiveOf = "", validation = "")
  var reference_sample_name: String = _

  /**
   * Short name of reference_sample_name
   * @return Short name of reference_sample_name
   */
  def refsample = this.reference_sample_name

  /**
   * Short name of reference_sample_name
   * @param value Short name of reference_sample_name
   */
  def refsample_=(value: String) { this.reference_sample_name = value }

  /** Min quality score to consider. Smaller numbers process faster. Default: Q1. */
  @Argument(fullName = "min_quality_score", shortName = "minqs", doc = "Min quality score to consider. Smaller numbers process faster. Default: Q1.", required = false, exclusiveOf = "", validation = "")
  var min_quality_score: Option[Byte] = None

  /**
   * Short name of min_quality_score
   * @return Short name of min_quality_score
   */
  def minqs = this.min_quality_score

  /**
   * Short name of min_quality_score
   * @param value Short name of min_quality_score
   */
  def minqs_=(value: Option[Byte]) { this.min_quality_score = value }

  /** Max quality score to consider. Smaller numbers process faster. Default: Q40. */
  @Argument(fullName = "max_quality_score", shortName = "maxqs", doc = "Max quality score to consider. Smaller numbers process faster. Default: Q40.", required = false, exclusiveOf = "", validation = "")
  var max_quality_score: Option[Byte] = None

  /**
   * Short name of max_quality_score
   * @return Short name of max_quality_score
   */
  def maxqs = this.max_quality_score

  /**
   * Short name of max_quality_score
   * @param value Short name of max_quality_score
   */
  def maxqs_=(value: Option[Byte]) { this.max_quality_score = value }

  /** Phred-Scaled prior quality of the site. Default: Q20. */
  @Argument(fullName = "site_quality_prior", shortName = "site_prior", doc = "Phred-Scaled prior quality of the site. Default: Q20.", required = false, exclusiveOf = "", validation = "")
  var site_quality_prior: Option[Byte] = None

  /**
   * Short name of site_quality_prior
   * @return Short name of site_quality_prior
   */
  def site_prior = this.site_quality_prior

  /**
   * Short name of site_quality_prior
   * @param value Short name of site_quality_prior
   */
  def site_prior_=(value: Option[Byte]) { this.site_quality_prior = value }

  /** The minimum confidence in the error model to make a call. Number should be between 0 (no power requirement) and 1 (maximum power required). */
  @Argument(fullName = "min_power_threshold_for_calling", shortName = "min_call_power", doc = "The minimum confidence in the error model to make a call. Number should be between 0 (no power requirement) and 1 (maximum power required).", required = false, exclusiveOf = "", validation = "")
  var min_power_threshold_for_calling: Option[Double] = None

  /**
   * Short name of min_power_threshold_for_calling
   * @return Short name of min_power_threshold_for_calling
   */
  def min_call_power = this.min_power_threshold_for_calling

  /**
   * Short name of min_power_threshold_for_calling
   * @param value Short name of min_power_threshold_for_calling
   */
  def min_call_power_=(value: Option[Double]) { this.min_power_threshold_for_calling = value }

  /** Format string for min_power_threshold_for_calling */
  @Argument(fullName = "min_power_threshold_for_callingFormat", shortName = "", doc = "Format string for min_power_threshold_for_calling", required = false, exclusiveOf = "", validation = "")
  var min_power_threshold_for_callingFormat: String = "%s"

  /** If provided, we will annotate records with the number of alternate alleles that were discovered (but not necessarily genotyped) at a given site */
  @Argument(fullName = "annotateNDA", shortName = "nda", doc = "If provided, we will annotate records with the number of alternate alleles that were discovered (but not necessarily genotyped) at a given site", required = false, exclusiveOf = "", validation = "")
  var annotateNDA: Boolean = _

  /**
   * Short name of annotateNDA
   * @return Short name of annotateNDA
   */
  def nda = this.annotateNDA

  /**
   * Short name of annotateNDA
   * @param value Short name of annotateNDA
   */
  def nda_=(value: Boolean) { this.annotateNDA = value }

  /** Heterozygosity value used to compute prior likelihoods for any locus */
  @Argument(fullName = "heterozygosity", shortName = "hets", doc = "Heterozygosity value used to compute prior likelihoods for any locus", required = false, exclusiveOf = "", validation = "")
  var heterozygosity: Option[Double] = None

  /**
   * Short name of heterozygosity
   * @return Short name of heterozygosity
   */
  def hets = this.heterozygosity

  /**
   * Short name of heterozygosity
   * @param value Short name of heterozygosity
   */
  def hets_=(value: Option[Double]) { this.heterozygosity = value }

  /** Format string for heterozygosity */
  @Argument(fullName = "heterozygosityFormat", shortName = "", doc = "Format string for heterozygosity", required = false, exclusiveOf = "", validation = "")
  var heterozygosityFormat: String = "%s"

  /** Heterozygosity for indel calling */
  @Argument(fullName = "indel_heterozygosity", shortName = "indelHeterozygosity", doc = "Heterozygosity for indel calling", required = false, exclusiveOf = "", validation = "")
  var indel_heterozygosity: Option[Double] = None

  /**
   * Short name of indel_heterozygosity
   * @return Short name of indel_heterozygosity
   */
  def indelHeterozygosity = this.indel_heterozygosity

  /**
   * Short name of indel_heterozygosity
   * @param value Short name of indel_heterozygosity
   */
  def indelHeterozygosity_=(value: Option[Double]) { this.indel_heterozygosity = value }

  /** Format string for indel_heterozygosity */
  @Argument(fullName = "indel_heterozygosityFormat", shortName = "", doc = "Format string for indel_heterozygosity", required = false, exclusiveOf = "", validation = "")
  var indel_heterozygosityFormat: String = "%s"

  /** The minimum phred-scaled confidence threshold at which variants should be called */
  @Argument(fullName = "standard_min_confidence_threshold_for_calling", shortName = "stand_call_conf", doc = "The minimum phred-scaled confidence threshold at which variants should be called", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_calling: Option[Double] = None

  /**
   * Short name of standard_min_confidence_threshold_for_calling
   * @return Short name of standard_min_confidence_threshold_for_calling
   */
  def stand_call_conf = this.standard_min_confidence_threshold_for_calling

  /**
   * Short name of standard_min_confidence_threshold_for_calling
   * @param value Short name of standard_min_confidence_threshold_for_calling
   */
  def stand_call_conf_=(value: Option[Double]) { this.standard_min_confidence_threshold_for_calling = value }

  /** Format string for standard_min_confidence_threshold_for_calling */
  @Argument(fullName = "standard_min_confidence_threshold_for_callingFormat", shortName = "", doc = "Format string for standard_min_confidence_threshold_for_calling", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_callingFormat: String = "%s"

  /** The minimum phred-scaled confidence threshold at which variants should be emitted (and filtered with LowQual if less than the calling threshold) */
  @Argument(fullName = "standard_min_confidence_threshold_for_emitting", shortName = "stand_emit_conf", doc = "The minimum phred-scaled confidence threshold at which variants should be emitted (and filtered with LowQual if less than the calling threshold)", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_emitting: Option[Double] = None

  /**
   * Short name of standard_min_confidence_threshold_for_emitting
   * @return Short name of standard_min_confidence_threshold_for_emitting
   */
  def stand_emit_conf = this.standard_min_confidence_threshold_for_emitting

  /**
   * Short name of standard_min_confidence_threshold_for_emitting
   * @param value Short name of standard_min_confidence_threshold_for_emitting
   */
  def stand_emit_conf_=(value: Option[Double]) { this.standard_min_confidence_threshold_for_emitting = value }

  /** Format string for standard_min_confidence_threshold_for_emitting */
  @Argument(fullName = "standard_min_confidence_threshold_for_emittingFormat", shortName = "", doc = "Format string for standard_min_confidence_threshold_for_emitting", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_emittingFormat: String = "%s"

  /** Maximum number of alternate alleles to genotype */
  @Argument(fullName = "max_alternate_alleles", shortName = "maxAltAlleles", doc = "Maximum number of alternate alleles to genotype", required = false, exclusiveOf = "", validation = "")
  var max_alternate_alleles: Option[Int] = None

  /**
   * Short name of max_alternate_alleles
   * @return Short name of max_alternate_alleles
   */
  def maxAltAlleles = this.max_alternate_alleles

  /**
   * Short name of max_alternate_alleles
   * @param value Short name of max_alternate_alleles
   */
  def maxAltAlleles_=(value: Option[Int]) { this.max_alternate_alleles = value }

  /** Input prior for calls */
  @Argument(fullName = "input_prior", shortName = "inputPrior", doc = "Input prior for calls", required = false, exclusiveOf = "", validation = "")
  var input_prior: Seq[Double] = Nil

  /**
   * Short name of input_prior
   * @return Short name of input_prior
   */
  def inputPrior = this.input_prior

  /**
   * Short name of input_prior
   * @param value Short name of input_prior
   */
  def inputPrior_=(value: Seq[Double]) { this.input_prior = value }

  /** Ploidy (number of chromosomes) per sample. For pooled data, set to (Number of samples in each pool * Sample Ploidy). */
  @Argument(fullName = "sample_ploidy", shortName = "ploidy", doc = "Ploidy (number of chromosomes) per sample. For pooled data, set to (Number of samples in each pool * Sample Ploidy).", required = false, exclusiveOf = "", validation = "")
  var sample_ploidy: Option[Int] = None

  /**
   * Short name of sample_ploidy
   * @return Short name of sample_ploidy
   */
  def ploidy = this.sample_ploidy

  /**
   * Short name of sample_ploidy
   * @param value Short name of sample_ploidy
   */
  def ploidy_=(value: Option[Int]) { this.sample_ploidy = value }

  /** Specifies how to determine the alternate alleles to use for genotyping */
  @Argument(fullName = "genotyping_mode", shortName = "gt_mode", doc = "Specifies how to determine the alternate alleles to use for genotyping", required = false, exclusiveOf = "", validation = "")
  var genotyping_mode: String = _

  /**
   * Short name of genotyping_mode
   * @return Short name of genotyping_mode
   */
  def gt_mode = this.genotyping_mode

  /**
   * Short name of genotyping_mode
   * @param value Short name of genotyping_mode
   */
  def gt_mode_=(value: String) { this.genotyping_mode = value }

  /** The set of alleles at which to genotype when --genotyping_mode is GENOTYPE_GIVEN_ALLELES */
  @Input(fullName = "alleles", shortName = "alleles", doc = "The set of alleles at which to genotype when --genotyping_mode is GENOTYPE_GIVEN_ALLELES", required = false, exclusiveOf = "", validation = "")
  var alleles: File = _

  /** Dependencies on the index of alleles */
  @Input(fullName = "allelesIndex", shortName = "", doc = "Dependencies on the index of alleles", required = false, exclusiveOf = "", validation = "")
  private var allelesIndex: Seq[File] = Nil

  /** Fraction of contamination in sequencing data (for all samples) to aggressively remove */
  @Argument(fullName = "contamination_fraction_to_filter", shortName = "contamination", doc = "Fraction of contamination in sequencing data (for all samples) to aggressively remove", required = false, exclusiveOf = "", validation = "")
  var contamination_fraction_to_filter: Option[Double] = None

  /**
   * Short name of contamination_fraction_to_filter
   * @return Short name of contamination_fraction_to_filter
   */
  def contamination = this.contamination_fraction_to_filter

  /**
   * Short name of contamination_fraction_to_filter
   * @param value Short name of contamination_fraction_to_filter
   */
  def contamination_=(value: Option[Double]) { this.contamination_fraction_to_filter = value }

  /** Format string for contamination_fraction_to_filter */
  @Argument(fullName = "contamination_fraction_to_filterFormat", shortName = "", doc = "Format string for contamination_fraction_to_filter", required = false, exclusiveOf = "", validation = "")
  var contamination_fraction_to_filterFormat: String = "%s"

  /** Tab-separated File containing fraction of contamination in sequencing data (per sample) to aggressively remove. Format should be \"<SampleID><TAB><Contamination>\" (Contamination is double) per line; No header. */
  @Argument(fullName = "contamination_fraction_per_sample_file", shortName = "contaminationFile", doc = "Tab-separated File containing fraction of contamination in sequencing data (per sample) to aggressively remove. Format should be \"<SampleID><TAB><Contamination>\" (Contamination is double) per line; No header.", required = false, exclusiveOf = "", validation = "")
  var contamination_fraction_per_sample_file: File = _

  /**
   * Short name of contamination_fraction_per_sample_file
   * @return Short name of contamination_fraction_per_sample_file
   */
  def contaminationFile = this.contamination_fraction_per_sample_file

  /**
   * Short name of contamination_fraction_per_sample_file
   * @param value Short name of contamination_fraction_per_sample_file
   */
  def contaminationFile_=(value: File) { this.contamination_fraction_per_sample_file = value }

  /** Non-reference probability calculation model to employ */
  @Argument(fullName = "p_nonref_model", shortName = "pnrm", doc = "Non-reference probability calculation model to employ", required = false, exclusiveOf = "", validation = "")
  var p_nonref_model: String = _

  /**
   * Short name of p_nonref_model
   * @return Short name of p_nonref_model
   */
  def pnrm = this.p_nonref_model

  /**
   * Short name of p_nonref_model
   * @param value Short name of p_nonref_model
   */
  def pnrm_=(value: String) { this.p_nonref_model = value }

  /** x */
  @Argument(fullName = "exactcallslog", shortName = "logExactCalls", doc = "x", required = false, exclusiveOf = "", validation = "")
  var exactcallslog: File = _

  /**
   * Short name of exactcallslog
   * @return Short name of exactcallslog
   */
  def logExactCalls = this.exactcallslog

  /**
   * Short name of exactcallslog
   * @param value Short name of exactcallslog
   */
  def logExactCalls_=(value: File) { this.exactcallslog = value }

  /** Specifies which type of calls we should output */
  @Argument(fullName = "output_mode", shortName = "out_mode", doc = "Specifies which type of calls we should output", required = false, exclusiveOf = "", validation = "")
  var output_mode: String = _

  /**
   * Short name of output_mode
   * @return Short name of output_mode
   */
  def out_mode = this.output_mode

  /**
   * Short name of output_mode
   * @param value Short name of output_mode
   */
  def out_mode_=(value: String) { this.output_mode = value }

  /** Annotate all sites with PLs */
  @Argument(fullName = "allSitePLs", shortName = "allSitePLs", doc = "Annotate all sites with PLs", required = false, exclusiveOf = "", validation = "")
  var allSitePLs: Boolean = _

  /** dbSNP file */
  @Input(fullName = "dbsnp", shortName = "D", doc = "dbSNP file", required = false, exclusiveOf = "", validation = "")
  var dbsnp: File = _

  /**
   * Short name of dbsnp
   * @return Short name of dbsnp
   */
  def D = this.dbsnp

  /**
   * Short name of dbsnp
   * @param value Short name of dbsnp
   */
  def D_=(value: File) { this.dbsnp = value }

  /** Dependencies on the index of dbsnp */
  @Input(fullName = "dbsnpIndex", shortName = "", doc = "Dependencies on the index of dbsnp", required = false, exclusiveOf = "", validation = "")
  private var dbsnpIndex: Seq[File] = Nil

  /** Comparison VCF file */
  @Input(fullName = "comp", shortName = "comp", doc = "Comparison VCF file", required = false, exclusiveOf = "", validation = "")
  var comp: Seq[File] = Nil

  /** Dependencies on any indexes of comp */
  @Input(fullName = "compIndexes", shortName = "", doc = "Dependencies on any indexes of comp", required = false, exclusiveOf = "", validation = "")
  private var compIndexes: Seq[File] = Nil

  /** File to which variants should be written */
  @Output(fullName = "out", shortName = "o", doc = "File to which variants should be written", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[CatVariantsGatherer])
  var out: File = _

  /**
   * Short name of out
   * @return Short name of out
   */
  def o = this.out

  /**
   * Short name of out
   * @param value Short name of out
   */
  def o_=(value: File) { this.out = value }

  /** Automatically generated index for out */
  @Output(fullName = "outIndex", shortName = "", doc = "Automatically generated index for out", required = false, exclusiveOf = "", validation = "")
  @Gather(enabled = false)
  private var outIndex: File = _

  /** If provided, only these samples will be emitted into the VCF, regardless of which samples are present in the BAM file */
  @Argument(fullName = "onlyEmitSamples", shortName = "onlyEmitSamples", doc = "If provided, only these samples will be emitted into the VCF, regardless of which samples are present in the BAM file", required = false, exclusiveOf = "", validation = "")
  var onlyEmitSamples: Seq[String] = Nil

  /** File to print all of the annotated and detailed debugging output */
  @Argument(fullName = "debug_file", shortName = "debug_file", doc = "File to print all of the annotated and detailed debugging output", required = false, exclusiveOf = "", validation = "")
  var debug_file: File = _

  /** File to print any relevant callability metrics output */
  @Argument(fullName = "metrics_file", shortName = "metrics", doc = "File to print any relevant callability metrics output", required = false, exclusiveOf = "", validation = "")
  var metrics_file: File = _

  /**
   * Short name of metrics_file
   * @return Short name of metrics_file
   */
  def metrics = this.metrics_file

  /**
   * Short name of metrics_file
   * @param value Short name of metrics_file
   */
  def metrics_=(value: File) { this.metrics_file = value }

  /** One or more specific annotations to apply to variant calls */
  @Argument(fullName = "annotation", shortName = "A", doc = "One or more specific annotations to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var annotation: Seq[String] = Nil

  /**
   * Short name of annotation
   * @return Short name of annotation
   */
  def A = this.annotation

  /**
   * Short name of annotation
   * @param value Short name of annotation
   */
  def A_=(value: Seq[String]) { this.annotation = value }

  /** One or more specific annotations to exclude */
  @Argument(fullName = "excludeAnnotation", shortName = "XA", doc = "One or more specific annotations to exclude", required = false, exclusiveOf = "", validation = "")
  var excludeAnnotation: Seq[String] = Nil

  /**
   * Short name of excludeAnnotation
   * @return Short name of excludeAnnotation
   */
  def XA = this.excludeAnnotation

  /**
   * Short name of excludeAnnotation
   * @param value Short name of excludeAnnotation
   */
  def XA_=(value: Seq[String]) { this.excludeAnnotation = value }

  /** One or more classes/groups of annotations to apply to variant calls.  The single value 'none' removes the default group */
  @Argument(fullName = "group", shortName = "G", doc = "One or more classes/groups of annotations to apply to variant calls.  The single value 'none' removes the default group", required = false, exclusiveOf = "", validation = "")
  var group: Seq[String] = Nil

  /**
   * Short name of group
   * @return Short name of group
   */
  def G = this.group

  /**
   * Short name of group
   * @param value Short name of group
   */
  def G_=(value: Seq[String]) { this.group = value }

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = _

  /**
   * Short name of filter_reads_with_N_cigar
   * @return Short name of filter_reads_with_N_cigar
   */
  def filterRNC = this.filter_reads_with_N_cigar

  /**
   * Short name of filter_reads_with_N_cigar
   * @param value Short name of filter_reads_with_N_cigar
   */
  def filterRNC_=(value: Boolean) { this.filter_reads_with_N_cigar = value }

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = _

  /**
   * Short name of filter_mismatching_base_and_quals
   * @return Short name of filter_mismatching_base_and_quals
   */
  def filterMBQ = this.filter_mismatching_base_and_quals

  /**
   * Short name of filter_mismatching_base_and_quals
   * @param value Short name of filter_mismatching_base_and_quals
   */
  def filterMBQ_=(value: Boolean) { this.filter_mismatching_base_and_quals = value }

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = _

  /**
   * Short name of filter_bases_not_stored
   * @return Short name of filter_bases_not_stored
   */
  def filterNoBases = this.filter_bases_not_stored

  /**
   * Short name of filter_bases_not_stored
   * @param value Short name of filter_bases_not_stored
   */
  def filterNoBases_=(value: Boolean) { this.filter_bases_not_stored = value }

  override def freezeFieldValues() {
    super.freezeFieldValues()
    if (reference_sample_calls != null)
      reference_sample_callsIndex :+= new File(reference_sample_calls.getPath + ".idx")
    if (alleles != null)
      allelesIndex :+= new File(alleles.getPath + ".idx")
    if (dbsnp != null)
      dbsnpIndex :+= new File(dbsnp.getPath + ".idx")
    compIndexes ++= comp.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (!org.broadinstitute.gatk.utils.commandline.ArgumentTypeDescriptor.isCompressed(out.getPath))
        outIndex = new File(out.getPath + ".idx")
  }

  override def cmdLine = super.cmdLine +
    optional("-glm", genotype_likelihoods_model, spaceSeparated = true, escape = true, format = "%s") +
    optional("-pcr_error", pcr_error_rate, spaceSeparated = true, escape = true, format = pcr_error_rateFormat) +
    conditional(computeSLOD, "-slod", escape = true, format = "%s") +
    optional("-pairHMM", pair_hmm_implementation, spaceSeparated = true, escape = true, format = "%s") +
    optional("-mbq", min_base_quality_score, spaceSeparated = true, escape = true, format = "%s") +
    optional("-deletions", max_deletion_fraction, spaceSeparated = true, escape = true, format = max_deletion_fractionFormat) +
    optional("-minIndelCnt", min_indel_count_for_genotyping, spaceSeparated = true, escape = true, format = "%s") +
    optional("-minIndelFrac", min_indel_fraction_per_sample, spaceSeparated = true, escape = true, format = min_indel_fraction_per_sampleFormat) +
    optional("-indelGCP", indelGapContinuationPenalty, spaceSeparated = true, escape = true, format = "%s") +
    optional("-indelGOP", indelGapOpenPenalty, spaceSeparated = true, escape = true, format = "%s") +
    optional("-indelHSize", indelHaplotypeSize, spaceSeparated = true, escape = true, format = "%s") +
    conditional(indelDebug, "-indelDebug", escape = true, format = "%s") +
    conditional(ignoreSNPAlleles, "-ignoreSNPAlleles", escape = true, format = "%s") +
    conditional(allReadsSP, "-dl", escape = true, format = "%s") +
    conditional(ignoreLaneInfo, "-ignoreLane", escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-referenceCalls", reference_sample_calls), reference_sample_calls, spaceSeparated = true, escape = true, format = "%s") +
    optional("-refsample", reference_sample_name, spaceSeparated = true, escape = true, format = "%s") +
    optional("-minqs", min_quality_score, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxqs", max_quality_score, spaceSeparated = true, escape = true, format = "%s") +
    optional("-site_prior", site_quality_prior, spaceSeparated = true, escape = true, format = "%s") +
    optional("-min_call_power", min_power_threshold_for_calling, spaceSeparated = true, escape = true, format = min_power_threshold_for_callingFormat) +
    conditional(annotateNDA, "-nda", escape = true, format = "%s") +
    optional("-hets", heterozygosity, spaceSeparated = true, escape = true, format = heterozygosityFormat) +
    optional("-indelHeterozygosity", indel_heterozygosity, spaceSeparated = true, escape = true, format = indel_heterozygosityFormat) +
    optional("-stand_call_conf", standard_min_confidence_threshold_for_calling, spaceSeparated = true, escape = true, format = standard_min_confidence_threshold_for_callingFormat) +
    optional("-stand_emit_conf", standard_min_confidence_threshold_for_emitting, spaceSeparated = true, escape = true, format = standard_min_confidence_threshold_for_emittingFormat) +
    optional("-maxAltAlleles", max_alternate_alleles, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-inputPrior", input_prior, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ploidy", sample_ploidy, spaceSeparated = true, escape = true, format = "%s") +
    optional("-gt_mode", genotyping_mode, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-alleles", alleles), alleles, spaceSeparated = true, escape = true, format = "%s") +
    optional("-contamination", contamination_fraction_to_filter, spaceSeparated = true, escape = true, format = contamination_fraction_to_filterFormat) +
    optional("-contaminationFile", contamination_fraction_per_sample_file, spaceSeparated = true, escape = true, format = "%s") +
    optional("-pnrm", p_nonref_model, spaceSeparated = true, escape = true, format = "%s") +
    optional("-logExactCalls", exactcallslog, spaceSeparated = true, escape = true, format = "%s") +
    optional("-out_mode", output_mode, spaceSeparated = true, escape = true, format = "%s") +
    conditional(allSitePLs, "-allSitePLs", escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-D", dbsnp), dbsnp, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-comp", comp, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-onlyEmitSamples", onlyEmitSamples, spaceSeparated = true, escape = true, format = "%s") +
    optional("-debug_file", debug_file, spaceSeparated = true, escape = true, format = "%s") +
    optional("-metrics", metrics_file, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-A", annotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-XA", excludeAnnotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-G", group, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}

object UnifiedGenotyper {
  def apply(root: Configurable, inputFiles: List[File], outputFile: File): UnifiedGenotyper = {
    val ug = new UnifiedGenotyper(root)
    ug.input_file = inputFiles
    ug.out = outputFile
    //if (ug.out.getName.endsWith(".vcf.gz")) ug.vcfIndex = new File(ug.out.getAbsolutePath + ".tbi")
    ug
  }
}
