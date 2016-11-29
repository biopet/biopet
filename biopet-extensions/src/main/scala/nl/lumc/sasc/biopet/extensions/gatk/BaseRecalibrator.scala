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
package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, Input }

class BaseRecalibrator(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "BaseRecalibrator"
  scatterClass = classOf[ContigScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** A database of known polymorphic sites */
  @Input(fullName = "knownSites", shortName = "knownSites", doc = "A database of known polymorphic sites", required = false, exclusiveOf = "", validation = "")
  var knownSites: List[File] = config("known_sites", default = Nil)

  /** Dependencies on any indexes of knownSites */
  @Input(fullName = "knownSitesIndexes", shortName = "", doc = "Dependencies on any indexes of knownSites", required = false, exclusiveOf = "", validation = "")
  private var knownSitesIndexes: Seq[File] = Nil

  /** The output recalibration table file to create */
  @Output(fullName = "out", shortName = "o", doc = "The output recalibration table file to create", required = true, exclusiveOf = "", validation = "") //TODO: check gathering
  @Gather(classOf[org.broadinstitute.gatk.engine.recalibration.BQSRGatherer])
  var out: File = _

  /** One or more covariates to be used in the recalibration. Can be specified multiple times */
  @Argument(fullName = "covariate", shortName = "cov", doc = "One or more covariates to be used in the recalibration. Can be specified multiple times", required = false, exclusiveOf = "", validation = "")
  var covariate: List[String] = config("covariate", default = Nil)

  /** Do not use the standard set of covariates, but rather just the ones listed using the -cov argument */
  @Argument(fullName = "no_standard_covs", shortName = "noStandard", doc = "Do not use the standard set of covariates, but rather just the ones listed using the -cov argument", required = false, exclusiveOf = "", validation = "")
  var no_standard_covs: Boolean = config("no_standard_covs", default = false)

  /** If specified, allows the recalibrator to be used without a dbsnp rod. Very unsafe and for expert users only. */
  @Argument(fullName = "run_without_dbsnp_potentially_ruining_quality", shortName = "run_without_dbsnp_potentially_ruining_quality", doc = "If specified, allows the recalibrator to be used without a dbsnp rod. Very unsafe and for expert users only.", required = false, exclusiveOf = "", validation = "")
  var run_without_dbsnp_potentially_ruining_quality: Boolean = config("run_without_dbsnp_potentially_ruining_quality", default = false)

  /** How should we recalibrate solid bases in which the reference was inserted? Options = DO_NOTHING, SET_Q_ZERO, SET_Q_ZERO_BASE_N, or REMOVE_REF_BIAS */
  @Argument(fullName = "solid_recal_mode", shortName = "sMode", doc = "How should we recalibrate solid bases in which the reference was inserted? Options = DO_NOTHING, SET_Q_ZERO, SET_Q_ZERO_BASE_N, or REMOVE_REF_BIAS", required = false, exclusiveOf = "", validation = "")
  var solid_recal_mode: Option[String] = config("solid_recal_mode")

  /** Defines the behavior of the recalibrator when it encounters no calls in the color space. Options = THROW_EXCEPTION, LEAVE_READ_UNRECALIBRATED, or PURGE_READ */
  @Argument(fullName = "solid_nocall_strategy", shortName = "solid_nocall_strategy", doc = "Defines the behavior of the recalibrator when it encounters no calls in the color space. Options = THROW_EXCEPTION, LEAVE_READ_UNRECALIBRATED, or PURGE_READ", required = false, exclusiveOf = "", validation = "")
  var solid_nocall_strategy: Option[String] = config("solid_nocall_strategy")

  /** Size of the k-mer context to be used for base mismatches */
  @Argument(fullName = "mismatches_context_size", shortName = "mcs", doc = "Size of the k-mer context to be used for base mismatches", required = false, exclusiveOf = "", validation = "")
  var mismatches_context_size: Option[Int] = config("mismatches_context_size")

  /** Size of the k-mer context to be used for base insertions and deletions */
  @Argument(fullName = "indels_context_size", shortName = "ics", doc = "Size of the k-mer context to be used for base insertions and deletions", required = false, exclusiveOf = "", validation = "")
  var indels_context_size: Option[Int] = config("indels_context_size")

  /** The maximum cycle value permitted for the Cycle covariate */
  @Argument(fullName = "maximum_cycle_value", shortName = "maxCycle", doc = "The maximum cycle value permitted for the Cycle covariate", required = false, exclusiveOf = "", validation = "")
  var maximum_cycle_value: Option[Int] = config("maximum_cycle_value")

  /** default quality for the base mismatches covariate */
  @Argument(fullName = "mismatches_default_quality", shortName = "mdq", doc = "default quality for the base mismatches covariate", required = false, exclusiveOf = "", validation = "")
  var mismatches_default_quality: Option[String] = config("mismatches_default_quality")

  /** default quality for the base insertions covariate */
  @Argument(fullName = "insertions_default_quality", shortName = "idq", doc = "default quality for the base insertions covariate", required = false, exclusiveOf = "", validation = "")
  var insertions_default_quality: Option[String] = config("insertions_default_quality")

  /** default quality for the base deletions covariate */
  @Argument(fullName = "deletions_default_quality", shortName = "ddq", doc = "default quality for the base deletions covariate", required = false, exclusiveOf = "", validation = "")
  var deletions_default_quality: Option[String] = config("deletions_default_quality")

  /** minimum quality for the bases in the tail of the reads to be considered */
  @Argument(fullName = "low_quality_tail", shortName = "lqt", doc = "minimum quality for the bases in the tail of the reads to be considered", required = false, exclusiveOf = "", validation = "")
  var low_quality_tail: Option[String] = config("low_quality_tail")

  /** number of distinct quality scores in the quantized output */
  @Argument(fullName = "quantizing_levels", shortName = "ql", doc = "number of distinct quality scores in the quantized output", required = false, exclusiveOf = "", validation = "")
  var quantizing_levels: Option[Int] = config("quantizing_levels")

  /** the binary tag covariate name if using it */
  @Argument(fullName = "binary_tag_name", shortName = "bintag", doc = "the binary tag covariate name if using it", required = false, exclusiveOf = "", validation = "")
  var binary_tag_name: Option[String] = config("binary_tag_name")

  /** Sort the rows in the tables of reports */
  @Argument(fullName = "sort_by_all_columns", shortName = "sortAllCols", doc = "Sort the rows in the tables of reports", required = false, exclusiveOf = "", validation = "")
  var sort_by_all_columns: Boolean = config("sort_by_all_columns", default = false)

  /** If a read has no platform then default to the provided String. Valid options are illumina, 454, and solid. */
  @Argument(fullName = "default_platform", shortName = "dP", doc = "If a read has no platform then default to the provided String. Valid options are illumina, 454, and solid.", required = false, exclusiveOf = "", validation = "")
  var default_platform: Option[String] = config("default_platform")

  /** If provided, the platform of EVERY read will be forced to be the provided String. Valid options are illumina, 454, and solid. */
  @Argument(fullName = "force_platform", shortName = "fP", doc = "If provided, the platform of EVERY read will be forced to be the provided String. Valid options are illumina, 454, and solid.", required = false, exclusiveOf = "", validation = "")
  var force_platform: Option[String] = config("force_platform")

  /** If provided, the read group of EVERY read will be forced to be the provided String. */
  @Argument(fullName = "force_readgroup", shortName = "fRG", doc = "If provided, the read group of EVERY read will be forced to be the provided String.", required = false, exclusiveOf = "", validation = "")
  var force_readgroup: Option[String] = config("force_readgroup")

  /** If provided, log all updates to the recalibration tables to the given file. For debugging/testing purposes only */
  @Output(fullName = "recal_table_update_log", shortName = "recal_table_update_log", doc = "If provided, log all updates to the recalibration tables to the given file. For debugging/testing purposes only", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var recal_table_update_log: File = _

  /** Max size of the k-mer context to be used for repeat covariates */
  @Argument(fullName = "max_str_unit_length", shortName = "maxstr", doc = "Max size of the k-mer context to be used for repeat covariates", required = false, exclusiveOf = "", validation = "")
  var max_str_unit_length: Option[Int] = config("max_str_unit_length")

  /** Max number of repetitions to be used for repeat covariates */
  @Argument(fullName = "max_repeat_length", shortName = "maxrep", doc = "Max number of repetitions to be used for repeat covariates", required = false, exclusiveOf = "", validation = "")
  var max_repeat_length: Option[Int] = config("max_repeat_length")

  /** Reduce memory usage in multi-threaded code at the expense of threading efficiency */
  @Argument(fullName = "lowMemoryMode", shortName = "lowMemoryMode", doc = "Reduce memory usage in multi-threaded code at the expense of threading efficiency", required = false, exclusiveOf = "", validation = "")
  var lowMemoryMode: Boolean = config("lowMemoryMode", default = false)

  /** BQSR BAQ gap open penalty (Phred Scaled).  Default value is 40.  30 is perhaps better for whole genome call sets */
  @Argument(fullName = "bqsrBAQGapOpenPenalty", shortName = "bqsrBAQGOP", doc = "BQSR BAQ gap open penalty (Phred Scaled).  Default value is 40.  30 is perhaps better for whole genome call sets", required = false, exclusiveOf = "", validation = "")
  var bqsrBAQGapOpenPenalty: Option[Double] = config("bqsrBAQGapOpenPenalty")

  /** Format string for bqsrBAQGapOpenPenalty */
  @Argument(fullName = "bqsrBAQGapOpenPenaltyFormat", shortName = "", doc = "Format string for bqsrBAQGapOpenPenalty", required = false, exclusiveOf = "", validation = "")
  var bqsrBAQGapOpenPenaltyFormat: String = "%s"

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = config("filter_reads_with_N_cigar", default = false)

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = config("filter_mismatching_base_and_quals", default = false)

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = config("filter_bases_not_stored", default = false)

  dbsnpVcfFile.foreach(knownSites :+= _)

  override def beforeGraph() {
    super.beforeGraph()
    knownSitesIndexes ++= knownSites.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
  }

  override def cmdLine = super.cmdLine +
    repeat("-knownSites", knownSites, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    required("-o", out, spaceSeparated = true, escape = true, format = "%s") +
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

object BaseRecalibrator {
  def apply(root: Configurable, input: File, output: File): BaseRecalibrator = {
    val br = new BaseRecalibrator(root)
    br.input_file :+= input
    br.out = output
    br
  }
}
