/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

//import java.io.File
//
//import nl.lumc.sasc.biopet.utils.config.Configurable
//
//class BaseRecalibrator(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.BaseRecalibrator with GatkGeneral {
//  if (config.contains("scattercount")) scatterCount = config("scattercount", default = 1)
//  if (config.contains("dbsnp")) knownSites :+= new File(config("dbsnp").asString)
//  if (config.contains("known_sites")) knownSites :+= new File(config("known_sites").asString)
//}
//
//object BaseRecalibrator {
//  def apply(root: Configurable, input: File, output: File): BaseRecalibrator = {
//    val br = new BaseRecalibrator(root)
//    br.input_file :+= input
//    br.out = output
//    br
//  }
//}

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ GATKScatterFunction, ReadScatterFunction, TaggedFile }
import org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, _ }

class BaseRecalibrator(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  analysisName = "BaseRecalibrator"
  analysis_type = "BaseRecalibrator"
  scatterClass = classOf[ReadScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** A database of known polymorphic sites */
  @Input(fullName = "knownSites", shortName = "knownSites", doc = "A database of known polymorphic sites", required = false, exclusiveOf = "", validation = "")
  var knownSites: Seq[File] = Nil

  /** Dependencies on any indexes of knownSites */
  @Input(fullName = "knownSitesIndexes", shortName = "", doc = "Dependencies on any indexes of knownSites", required = false, exclusiveOf = "", validation = "")
  private var knownSitesIndexes: Seq[File] = Nil

  /** The output recalibration table file to create */
  @Output(fullName = "out", shortName = "o", doc = "The output recalibration table file to create", required = true, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.engine.recalibration.BQSRGatherer])
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

  /** List the available covariates and exit */
  @Argument(fullName = "list", shortName = "ls", doc = "List the available covariates and exit", required = false, exclusiveOf = "", validation = "")
  var list: Boolean = _

  /**
   * Short name of list
   * @return Short name of list
   */
  def ls = this.list

  /**
   * Short name of list
   * @param value Short name of list
   */
  def ls_=(value: Boolean) { this.list = value }

  /** One or more covariates to be used in the recalibration. Can be specified multiple times */
  @Argument(fullName = "covariate", shortName = "cov", doc = "One or more covariates to be used in the recalibration. Can be specified multiple times", required = false, exclusiveOf = "", validation = "")
  var covariate: Seq[String] = Nil

  /**
   * Short name of covariate
   * @return Short name of covariate
   */
  def cov = this.covariate

  /**
   * Short name of covariate
   * @param value Short name of covariate
   */
  def cov_=(value: Seq[String]) { this.covariate = value }

  /** Do not use the standard set of covariates, but rather just the ones listed using the -cov argument */
  @Argument(fullName = "no_standard_covs", shortName = "noStandard", doc = "Do not use the standard set of covariates, but rather just the ones listed using the -cov argument", required = false, exclusiveOf = "", validation = "")
  var no_standard_covs: Boolean = _

  /**
   * Short name of no_standard_covs
   * @return Short name of no_standard_covs
   */
  def noStandard = this.no_standard_covs

  /**
   * Short name of no_standard_covs
   * @param value Short name of no_standard_covs
   */
  def noStandard_=(value: Boolean) { this.no_standard_covs = value }

  /** If specified, allows the recalibrator to be used without a dbsnp rod. Very unsafe and for expert users only. */
  @Argument(fullName = "run_without_dbsnp_potentially_ruining_quality", shortName = "run_without_dbsnp_potentially_ruining_quality", doc = "If specified, allows the recalibrator to be used without a dbsnp rod. Very unsafe and for expert users only.", required = false, exclusiveOf = "", validation = "")
  var run_without_dbsnp_potentially_ruining_quality: Boolean = _

  /** How should we recalibrate solid bases in which the reference was inserted? Options = DO_NOTHING, SET_Q_ZERO, SET_Q_ZERO_BASE_N, or REMOVE_REF_BIAS */
  @Argument(fullName = "solid_recal_mode", shortName = "sMode", doc = "How should we recalibrate solid bases in which the reference was inserted? Options = DO_NOTHING, SET_Q_ZERO, SET_Q_ZERO_BASE_N, or REMOVE_REF_BIAS", required = false, exclusiveOf = "", validation = "")
  var solid_recal_mode: String = _

  /**
   * Short name of solid_recal_mode
   * @return Short name of solid_recal_mode
   */
  def sMode = this.solid_recal_mode

  /**
   * Short name of solid_recal_mode
   * @param value Short name of solid_recal_mode
   */
  def sMode_=(value: String) { this.solid_recal_mode = value }

  /** Defines the behavior of the recalibrator when it encounters no calls in the color space. Options = THROW_EXCEPTION, LEAVE_READ_UNRECALIBRATED, or PURGE_READ */
  @Argument(fullName = "solid_nocall_strategy", shortName = "solid_nocall_strategy", doc = "Defines the behavior of the recalibrator when it encounters no calls in the color space. Options = THROW_EXCEPTION, LEAVE_READ_UNRECALIBRATED, or PURGE_READ", required = false, exclusiveOf = "", validation = "")
  var solid_nocall_strategy: String = _

  /** Size of the k-mer context to be used for base mismatches */
  @Argument(fullName = "mismatches_context_size", shortName = "mcs", doc = "Size of the k-mer context to be used for base mismatches", required = false, exclusiveOf = "", validation = "")
  var mismatches_context_size: Option[Int] = None

  /**
   * Short name of mismatches_context_size
   * @return Short name of mismatches_context_size
   */
  def mcs = this.mismatches_context_size

  /**
   * Short name of mismatches_context_size
   * @param value Short name of mismatches_context_size
   */
  def mcs_=(value: Option[Int]) { this.mismatches_context_size = value }

  /** Size of the k-mer context to be used for base insertions and deletions */
  @Argument(fullName = "indels_context_size", shortName = "ics", doc = "Size of the k-mer context to be used for base insertions and deletions", required = false, exclusiveOf = "", validation = "")
  var indels_context_size: Option[Int] = None

  /**
   * Short name of indels_context_size
   * @return Short name of indels_context_size
   */
  def ics = this.indels_context_size

  /**
   * Short name of indels_context_size
   * @param value Short name of indels_context_size
   */
  def ics_=(value: Option[Int]) { this.indels_context_size = value }

  /** The maximum cycle value permitted for the Cycle covariate */
  @Argument(fullName = "maximum_cycle_value", shortName = "maxCycle", doc = "The maximum cycle value permitted for the Cycle covariate", required = false, exclusiveOf = "", validation = "")
  var maximum_cycle_value: Option[Int] = None

  /**
   * Short name of maximum_cycle_value
   * @return Short name of maximum_cycle_value
   */
  def maxCycle = this.maximum_cycle_value

  /**
   * Short name of maximum_cycle_value
   * @param value Short name of maximum_cycle_value
   */
  def maxCycle_=(value: Option[Int]) { this.maximum_cycle_value = value }

  /** default quality for the base mismatches covariate */
  @Argument(fullName = "mismatches_default_quality", shortName = "mdq", doc = "default quality for the base mismatches covariate", required = false, exclusiveOf = "", validation = "")
  var mismatches_default_quality: Option[Byte] = None

  /**
   * Short name of mismatches_default_quality
   * @return Short name of mismatches_default_quality
   */
  def mdq = this.mismatches_default_quality

  /**
   * Short name of mismatches_default_quality
   * @param value Short name of mismatches_default_quality
   */
  def mdq_=(value: Option[Byte]) { this.mismatches_default_quality = value }

  /** default quality for the base insertions covariate */
  @Argument(fullName = "insertions_default_quality", shortName = "idq", doc = "default quality for the base insertions covariate", required = false, exclusiveOf = "", validation = "")
  var insertions_default_quality: Option[Byte] = None

  /**
   * Short name of insertions_default_quality
   * @return Short name of insertions_default_quality
   */
  def idq = this.insertions_default_quality

  /**
   * Short name of insertions_default_quality
   * @param value Short name of insertions_default_quality
   */
  def idq_=(value: Option[Byte]) { this.insertions_default_quality = value }

  /** default quality for the base deletions covariate */
  @Argument(fullName = "deletions_default_quality", shortName = "ddq", doc = "default quality for the base deletions covariate", required = false, exclusiveOf = "", validation = "")
  var deletions_default_quality: Option[Byte] = None

  /**
   * Short name of deletions_default_quality
   * @return Short name of deletions_default_quality
   */
  def ddq = this.deletions_default_quality

  /**
   * Short name of deletions_default_quality
   * @param value Short name of deletions_default_quality
   */
  def ddq_=(value: Option[Byte]) { this.deletions_default_quality = value }

  /** minimum quality for the bases in the tail of the reads to be considered */
  @Argument(fullName = "low_quality_tail", shortName = "lqt", doc = "minimum quality for the bases in the tail of the reads to be considered", required = false, exclusiveOf = "", validation = "")
  var low_quality_tail: Option[Byte] = None

  /**
   * Short name of low_quality_tail
   * @return Short name of low_quality_tail
   */
  def lqt = this.low_quality_tail

  /**
   * Short name of low_quality_tail
   * @param value Short name of low_quality_tail
   */
  def lqt_=(value: Option[Byte]) { this.low_quality_tail = value }

  /** number of distinct quality scores in the quantized output */
  @Argument(fullName = "quantizing_levels", shortName = "ql", doc = "number of distinct quality scores in the quantized output", required = false, exclusiveOf = "", validation = "")
  var quantizing_levels: Option[Int] = None

  /**
   * Short name of quantizing_levels
   * @return Short name of quantizing_levels
   */
  def ql = this.quantizing_levels

  /**
   * Short name of quantizing_levels
   * @param value Short name of quantizing_levels
   */
  def ql_=(value: Option[Int]) { this.quantizing_levels = value }

  /** the binary tag covariate name if using it */
  @Argument(fullName = "binary_tag_name", shortName = "bintag", doc = "the binary tag covariate name if using it", required = false, exclusiveOf = "", validation = "")
  var binary_tag_name: String = _

  /**
   * Short name of binary_tag_name
   * @return Short name of binary_tag_name
   */
  def bintag = this.binary_tag_name

  /**
   * Short name of binary_tag_name
   * @param value Short name of binary_tag_name
   */
  def bintag_=(value: String) { this.binary_tag_name = value }

  /** Sort the rows in the tables of reports */
  @Argument(fullName = "sort_by_all_columns", shortName = "sortAllCols", doc = "Sort the rows in the tables of reports", required = false, exclusiveOf = "", validation = "")
  var sort_by_all_columns: Boolean = _

  /**
   * Short name of sort_by_all_columns
   * @return Short name of sort_by_all_columns
   */
  def sortAllCols = this.sort_by_all_columns

  /**
   * Short name of sort_by_all_columns
   * @param value Short name of sort_by_all_columns
   */
  def sortAllCols_=(value: Boolean) { this.sort_by_all_columns = value }

  /** If a read has no platform then default to the provided String. Valid options are illumina, 454, and solid. */
  @Argument(fullName = "default_platform", shortName = "dP", doc = "If a read has no platform then default to the provided String. Valid options are illumina, 454, and solid.", required = false, exclusiveOf = "", validation = "")
  var default_platform: String = _

  /**
   * Short name of default_platform
   * @return Short name of default_platform
   */
  def dP = this.default_platform

  /**
   * Short name of default_platform
   * @param value Short name of default_platform
   */
  def dP_=(value: String) { this.default_platform = value }

  /** If provided, the platform of EVERY read will be forced to be the provided String. Valid options are illumina, 454, and solid. */
  @Argument(fullName = "force_platform", shortName = "fP", doc = "If provided, the platform of EVERY read will be forced to be the provided String. Valid options are illumina, 454, and solid.", required = false, exclusiveOf = "", validation = "")
  var force_platform: String = _

  /**
   * Short name of force_platform
   * @return Short name of force_platform
   */
  def fP = this.force_platform

  /**
   * Short name of force_platform
   * @param value Short name of force_platform
   */
  def fP_=(value: String) { this.force_platform = value }

  /** If provided, the read group of EVERY read will be forced to be the provided String. */
  @Argument(fullName = "force_readgroup", shortName = "fRG", doc = "If provided, the read group of EVERY read will be forced to be the provided String.", required = false, exclusiveOf = "", validation = "")
  var force_readgroup: String = _

  /**
   * Short name of force_readgroup
   * @return Short name of force_readgroup
   */
  def fRG = this.force_readgroup

  /**
   * Short name of force_readgroup
   * @param value Short name of force_readgroup
   */
  def fRG_=(value: String) { this.force_readgroup = value }

  /** If provided, log all updates to the recalibration tables to the given file. For debugging/testing purposes only */
  @Output(fullName = "recal_table_update_log", shortName = "recal_table_update_log", doc = "If provided, log all updates to the recalibration tables to the given file. For debugging/testing purposes only", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var recal_table_update_log: File = _

  /** Max size of the k-mer context to be used for repeat covariates */
  @Argument(fullName = "max_str_unit_length", shortName = "maxstr", doc = "Max size of the k-mer context to be used for repeat covariates", required = false, exclusiveOf = "", validation = "")
  var max_str_unit_length: Option[Int] = None

  /**
   * Short name of max_str_unit_length
   * @return Short name of max_str_unit_length
   */
  def maxstr = this.max_str_unit_length

  /**
   * Short name of max_str_unit_length
   * @param value Short name of max_str_unit_length
   */
  def maxstr_=(value: Option[Int]) { this.max_str_unit_length = value }

  /** Max number of repetitions to be used for repeat covariates */
  @Argument(fullName = "max_repeat_length", shortName = "maxrep", doc = "Max number of repetitions to be used for repeat covariates", required = false, exclusiveOf = "", validation = "")
  var max_repeat_length: Option[Int] = None

  /**
   * Short name of max_repeat_length
   * @return Short name of max_repeat_length
   */
  def maxrep = this.max_repeat_length

  /**
   * Short name of max_repeat_length
   * @param value Short name of max_repeat_length
   */
  def maxrep_=(value: Option[Int]) { this.max_repeat_length = value }

  /** Reduce memory usage in multi-threaded code at the expense of threading efficiency */
  @Argument(fullName = "lowMemoryMode", shortName = "lowMemoryMode", doc = "Reduce memory usage in multi-threaded code at the expense of threading efficiency", required = false, exclusiveOf = "", validation = "")
  var lowMemoryMode: Boolean = _

  /** BQSR BAQ gap open penalty (Phred Scaled).  Default value is 40.  30 is perhaps better for whole genome call sets */
  @Argument(fullName = "bqsrBAQGapOpenPenalty", shortName = "bqsrBAQGOP", doc = "BQSR BAQ gap open penalty (Phred Scaled).  Default value is 40.  30 is perhaps better for whole genome call sets", required = false, exclusiveOf = "", validation = "")
  var bqsrBAQGapOpenPenalty: Option[Double] = None

  /**
   * Short name of bqsrBAQGapOpenPenalty
   * @return Short name of bqsrBAQGapOpenPenalty
   */
  def bqsrBAQGOP = this.bqsrBAQGapOpenPenalty

  /**
   * Short name of bqsrBAQGapOpenPenalty
   * @param value Short name of bqsrBAQGapOpenPenalty
   */
  def bqsrBAQGOP_=(value: Option[Double]) { this.bqsrBAQGapOpenPenalty = value }

  /** Format string for bqsrBAQGapOpenPenalty */
  @Argument(fullName = "bqsrBAQGapOpenPenaltyFormat", shortName = "", doc = "Format string for bqsrBAQGapOpenPenalty", required = false, exclusiveOf = "", validation = "")
  var bqsrBAQGapOpenPenaltyFormat: String = "%s"

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
    knownSitesIndexes ++= knownSites.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
  }

  override def cmdLine = super.cmdLine +
    repeat("-knownSites", knownSites, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    required("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    conditional(list, "-ls", escape = true, format = "%s") +
    repeat("-cov", covariate, spaceSeparated = true, escape = true, format = "%s") +
    conditional(no_standard_covs, "-noStandard", escape = true, format = "%s") +
    conditional(run_without_dbsnp_potentially_ruining_quality, "-run_without_dbsnp_potentially_ruining_quality", escape = true, format = "%s") +
    optional("-sMode", solid_recal_mode, spaceSeparated = true, escape = true, format = "%s") +
    optional("-solid_nocall_strategy", solid_nocall_strategy, spaceSeparated = true, escape = true, format = "%s") +
    optional("-mcs", mismatches_context_size, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ics", indels_context_size, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxCycle", maximum_cycle_value, spaceSeparated = true, escape = true, format = "%s") +
    optional("-mdq", mismatches_default_quality, spaceSeparated = true, escape = true, format = "%s") +
    optional("-idq", insertions_default_quality, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ddq", deletions_default_quality, spaceSeparated = true, escape = true, format = "%s") +
    optional("-lqt", low_quality_tail, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ql", quantizing_levels, spaceSeparated = true, escape = true, format = "%s") +
    optional("-bintag", binary_tag_name, spaceSeparated = true, escape = true, format = "%s") +
    conditional(sort_by_all_columns, "-sortAllCols", escape = true, format = "%s") +
    optional("-dP", default_platform, spaceSeparated = true, escape = true, format = "%s") +
    optional("-fP", force_platform, spaceSeparated = true, escape = true, format = "%s") +
    optional("-fRG", force_readgroup, spaceSeparated = true, escape = true, format = "%s") +
    optional("-recal_table_update_log", recal_table_update_log, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxstr", max_str_unit_length, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxrep", max_repeat_length, spaceSeparated = true, escape = true, format = "%s") +
    conditional(lowMemoryMode, "-lowMemoryMode", escape = true, format = "%s") +
    optional("-bqsrBAQGOP", bqsrBAQGapOpenPenalty, spaceSeparated = true, escape = true, format = bqsrBAQGapOpenPenaltyFormat) +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
