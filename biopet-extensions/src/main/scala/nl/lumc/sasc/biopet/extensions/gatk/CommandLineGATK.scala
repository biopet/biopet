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

import nl.lumc.sasc.biopet.core.{BiopetJavaCommandLineFunction, Reference, Version}
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{Argument, Gather, Input, Output}
import org.broadinstitute.gatk.utils.interval.{IntervalMergingRule, IntervalSetRule}

import scala.util.matching.Regex

trait CommandLineGATK extends BiopetJavaCommandLineFunction with Reference with Version {
  analysisName = analysis_type
  javaMainClass = "org.broadinstitute.gatk.engine.CommandLineGATK"
  jarFile = config("gatk_jar")

  /** Name of the tool to run */
  def analysis_type: String

  /** Input file containing sequence data (BAM or CRAM) */
  @Input(fullName = "input_file", shortName = "I", doc = "Input file containing sequence data (BAM or CRAM)", required = false, exclusiveOf = "", validation = "")
  var input_file: Seq[File] = Nil

  /** Dependencies on any indexes of input_file */
  @Input(fullName = "input_fileIndexes", shortName = "", doc = "Dependencies on any indexes of input_file", required = false, exclusiveOf = "", validation = "")
  private var input_fileIndexes: Seq[File] = Nil

  /** Emit a log entry (level INFO) containing the full list of sequence data files to be included in the analysis (including files inside .bam.list or .cram.list files). */
  @Argument(fullName = "showFullBamList", shortName = "", doc = "Emit a log entry (level INFO) containing the full list of sequence data files to be included in the analysis (including files inside .bam.list or .cram.list files).", required = false, exclusiveOf = "", validation = "")
  var showFullBamList: Boolean = config("showFullBamList", default = false)

  /** Number of reads per SAM file to buffer in memory */
  @Argument(fullName = "read_buffer_size", shortName = "rbs", doc = "Number of reads per SAM file to buffer in memory", required = false, exclusiveOf = "", validation = "")
  var read_buffer_size: Option[Int] = config("read_buffer_size")

  /** Run reporting mode */
  @Argument(fullName = "phone_home", shortName = "et", doc = "Run reporting mode", required = false, exclusiveOf = "", validation = "")
  var phone_home: Option[String] = config("phone_home")

  /** GATK key file required to run with -et NO_ET */
  @Input(fullName = "gatk_key", shortName = "K", doc = "GATK key file required to run with -et NO_ET", required = false, exclusiveOf = "", validation = "")
  var gatk_key: Option[File] = config("gatk_key")

  /** Tag to identify this GATK run as part of a group of runs */
  @Argument(fullName = "tag", shortName = "tag", doc = "Tag to identify this GATK run as part of a group of runs", required = false, exclusiveOf = "", validation = "")
  var tag: Option[String] = config("tag")

  /** Filters to apply to reads before analysis */
  @Argument(fullName = "read_filter", shortName = "rf", doc = "Filters to apply to reads before analysis", required = false, exclusiveOf = "", validation = "")
  var read_filter: List[String] = config("read_filter", default = Nil)

  /** Read filters to disable */
  @Argument(fullName = "disable_read_filter", shortName = "drf", doc = "Read filters to disable", required = false, exclusiveOf = "", validation = "")
  var disable_read_filter: List[String] = config("disable_read_filter", default = Nil)

  /** One or more genomic intervals over which to operate */
  @Input(fullName = "intervals", shortName = "L", doc = "One or more genomic intervals over which to operate", required = false, exclusiveOf = "intervalsString", validation = "")
  var intervals: List[File] = config("intervals", default = Nil)

  /** One or more genomic intervals over which to operate */
  @Argument(fullName = "intervalsString", shortName = "L", doc = "One or more genomic intervals over which to operate", required = false, exclusiveOf = "intervals", validation = "")
  var intervalsString: List[String] = config("intervalsString", default = Nil)

  /** One or more genomic intervals to exclude from processing */
  @Input(fullName = "excludeIntervals", shortName = "XL", doc = "One or more genomic intervals to exclude from processing", required = false, exclusiveOf = "excludeIntervalsString", validation = "")
  var excludeIntervals: List[File] = config("excludeIntervals", default = Nil)

  /** One or more genomic intervals to exclude from processing */
  @Argument(fullName = "excludeIntervalsString", shortName = "XL", doc = "One or more genomic intervals to exclude from processing", required = false, exclusiveOf = "excludeIntervals", validation = "")
  var excludeIntervalsString: List[String] = config("excludeIntervalsString", default = Nil)

  /** Set merging approach to use for combining interval inputs */
  @Argument(fullName = "interval_set_rule", shortName = "isr", doc = "Set merging approach to use for combining interval inputs", required = false, exclusiveOf = "", validation = "")
  var interval_set_rule: Option[IntervalSetRule] = None

  /** Interval merging rule for abutting intervals */
  @Argument(fullName = "interval_merging", shortName = "im", doc = "Interval merging rule for abutting intervals", required = false, exclusiveOf = "", validation = "")
  var interval_merging: Option[IntervalMergingRule] = None

  /** Amount of padding (in bp) to add to each interval */
  @Argument(fullName = "interval_padding", shortName = "ip", doc = "Amount of padding (in bp) to add to each interval", required = false, exclusiveOf = "", validation = "")
  var interval_padding: Option[Int] = config("interval_padding")

  /** Reference sequence file */
  @Input(fullName = "reference_sequence", shortName = "R", doc = "Reference sequence file", required = false, exclusiveOf = "", validation = "")
  var reference_sequence: File = _

  /** Use a non-deterministic random seed */
  @Argument(fullName = "nonDeterministicRandomSeed", shortName = "ndrs", doc = "Use a non-deterministic random seed", required = false, exclusiveOf = "", validation = "")
  var nonDeterministicRandomSeed: Boolean = config("nonDeterministicRandomSeed", default = false)

  /** Completely eliminates randomized dithering from rank sum tests. */
  @Argument(fullName = "disableDithering", shortName = "", doc = "Completely eliminates randomized dithering from rank sum tests.", required = false, exclusiveOf = "", validation = "")
  var disableDithering: Boolean = config("disableDithering", default = false)

  /** Stop execution cleanly as soon as maxRuntime has been reached */
  @Argument(fullName = "maxRuntime", shortName = "maxRuntime", doc = "Stop execution cleanly as soon as maxRuntime has been reached", required = false, exclusiveOf = "", validation = "")
  var maxRuntime: Option[Long] = config("maxRuntime")

  /** Unit of time used by maxRuntime */
  @Argument(fullName = "maxRuntimeUnits", shortName = "maxRuntimeUnits", doc = "Unit of time used by maxRuntime", required = false, exclusiveOf = "", validation = "")
  var maxRuntimeUnits: Option[String] = config("maxRuntimeUnits")

  /** Type of read downsampling to employ at a given locus */
  @Argument(fullName = "downsampling_type", shortName = "dt", doc = "Type of read downsampling to employ at a given locus", required = false, exclusiveOf = "", validation = "")
  var downsampling_type: Option[String] = config("downsampling_type")

  /** Fraction of reads to downsample to */
  @Argument(fullName = "downsample_to_fraction", shortName = "dfrac", doc = "Fraction of reads to downsample to", required = false, exclusiveOf = "", validation = "")
  var downsample_to_fraction: Option[Double] = config("downsample_to_fraction")

  /** Format string for downsample_to_fraction */
  @Argument(fullName = "downsample_to_fractionFormat", shortName = "", doc = "Format string for downsample_to_fraction", required = false, exclusiveOf = "", validation = "")
  var downsample_to_fractionFormat: String = "%s"

  /** Target coverage threshold for downsampling to coverage */
  @Argument(fullName = "downsample_to_coverage", shortName = "dcov", doc = "Target coverage threshold for downsampling to coverage", required = false, exclusiveOf = "", validation = "")
  var downsample_to_coverage: Option[Int] = config("downsample_to_coverage")

  /** Type of BAQ calculation to apply in the engine */
  @Argument(fullName = "baq", shortName = "baq", doc = "Type of BAQ calculation to apply in the engine", required = false, exclusiveOf = "", validation = "")
  var baq: Option[String] = config("baq")

  /** BAQ gap open penalty */
  @Argument(fullName = "baqGapOpenPenalty", shortName = "baqGOP", doc = "BAQ gap open penalty", required = false, exclusiveOf = "", validation = "")
  var baqGapOpenPenalty: Option[Double] = config("baqGapOpenPenalty")

  /** Format string for baqGapOpenPenalty */
  @Argument(fullName = "baqGapOpenPenaltyFormat", shortName = "", doc = "Format string for baqGapOpenPenalty", required = false, exclusiveOf = "", validation = "")
  var baqGapOpenPenaltyFormat: String = "%s"

  /** Reduce NDN elements in CIGAR string */
  @Argument(fullName = "refactor_NDN_cigar_string", shortName = "fixNDN", doc = "Reduce NDN elements in CIGAR string", required = false, exclusiveOf = "", validation = "")
  var refactor_NDN_cigar_string: Boolean = config("refactor_NDN_cigar_string", default = false)

  /** Fix mis-encoded base quality scores */
  @Argument(fullName = "fix_misencoded_quality_scores", shortName = "fixMisencodedQuals", doc = "Fix mis-encoded base quality scores", required = false, exclusiveOf = "", validation = "")
  var fix_misencoded_quality_scores: Boolean = config("fix_misencoded_quality_scores", default = false)

  /** Ignore warnings about base quality score encoding */
  @Argument(fullName = "allow_potentially_misencoded_quality_scores", shortName = "allowPotentiallyMisencodedQuals", doc = "Ignore warnings about base quality score encoding", required = false, exclusiveOf = "", validation = "")
  var allow_potentially_misencoded_quality_scores: Boolean = config("allow_potentially_misencoded_quality_scores", default = false)

  /** Use the base quality scores from the OQ tag */
  @Argument(fullName = "useOriginalQualities", shortName = "OQ", doc = "Use the base quality scores from the OQ tag", required = false, exclusiveOf = "", validation = "")
  var useOriginalQualities: Boolean = config("useOriginalQualities", default = false)

  /** Assign a default base quality */
  @Argument(fullName = "defaultBaseQualities", shortName = "DBQ", doc = "Assign a default base quality", required = false, exclusiveOf = "", validation = "")
  var defaultBaseQualities: Option[Int] = config("defaultBaseQualities")

  /** Write GATK runtime performance log to this file */
  @Output(fullName = "performanceLog", shortName = "PF", doc = "Write GATK runtime performance log to this file", required = false, exclusiveOf = "", validation = "")
  var performanceLog: Option[File] = None

  /** Input covariates table file for on-the-fly base quality score recalibration */
  @Input(fullName = "BQSR", shortName = "BQSR", doc = "Input covariates table file for on-the-fly base quality score recalibration", required = false, exclusiveOf = "", validation = "")
  var BQSR: Option[File] = _

  /** Quantize quality scores to a given number of levels (with -BQSR) */
  @Argument(fullName = "quantize_quals", shortName = "qq", doc = "Quantize quality scores to a given number of levels (with -BQSR)", required = false, exclusiveOf = "", validation = "")
  var quantize_quals: Option[Int] = config("quantize_quals")

  /** Use static quantized quality scores to a given number of levels (with -BQSR) */
  @Argument(fullName = "static_quantized_quals", shortName = "SQQ", doc = "Use static quantized quality scores to a given number of levels (with -BQSR)", required = false, exclusiveOf = "quantize_quals", validation = "")
  var static_quantized_quals: List[Int] = config("static_quantized_quals", default = Nil)

  /** Round quals down to nearest quantized qual */
  @Argument(fullName = "round_down_quantized", shortName = "RDQ", doc = "Round quals down to nearest quantized qual", required = false, exclusiveOf = "quantize_quals", validation = "")
  var round_down_quantized: Boolean = config("round_down_quantized", default = false)

  /** Disable printing of base insertion and deletion tags (with -BQSR) */
  @Argument(fullName = "disable_indel_quals", shortName = "DIQ", doc = "Disable printing of base insertion and deletion tags (with -BQSR)", required = false, exclusiveOf = "", validation = "")
  var disable_indel_quals: Boolean = config("disable_indel_quals", default = false)

  /** Emit the OQ tag with the original base qualities (with -BQSR) */
  @Argument(fullName = "emit_original_quals", shortName = "EOQ", doc = "Emit the OQ tag with the original base qualities (with -BQSR)", required = false, exclusiveOf = "", validation = "")
  var emit_original_quals: Boolean = config("emit_original_quals", default = false)

  /** Don't recalibrate bases with quality scores less than this threshold (with -BQSR) */
  @Argument(fullName = "preserve_qscores_less_than", shortName = "preserveQ", doc = "Don't recalibrate bases with quality scores less than this threshold (with -BQSR)", required = false, exclusiveOf = "", validation = "")
  var preserve_qscores_less_than: Option[Int] = config("preserve_qscores_less_than")

  /** Global Qscore Bayesian prior to use for BQSR */
  @Argument(fullName = "globalQScorePrior", shortName = "globalQScorePrior", doc = "Global Qscore Bayesian prior to use for BQSR", required = false, exclusiveOf = "", validation = "")
  var globalQScorePrior: Option[Double] = config("globalQScorePrior")

  /** Format string for globalQScorePrior */
  @Argument(fullName = "globalQScorePriorFormat", shortName = "", doc = "Format string for globalQScorePrior", required = false, exclusiveOf = "", validation = "")
  var globalQScorePriorFormat: String = "%s"

  /** How strict should we be with validation */
  @Argument(fullName = "validation_strictness", shortName = "S", doc = "How strict should we be with validation", required = false, exclusiveOf = "", validation = "")
  var validation_strictness: Option[String] = config("validation_strictness")

  /** Remove program records from the SAM header */
  @Argument(fullName = "remove_program_records", shortName = "rpr", doc = "Remove program records from the SAM header", required = false, exclusiveOf = "", validation = "")
  var remove_program_records: Boolean = config("remove_program_records", default = false)

  /** Keep program records in the SAM header */
  @Argument(fullName = "keep_program_records", shortName = "kpr", doc = "Keep program records in the SAM header", required = false, exclusiveOf = "", validation = "")
  var keep_program_records: Boolean = config("keep_program_records", default = false)

  /** Rename sample IDs on-the-fly at runtime using the provided mapping file */
  @Input(fullName = "sample_rename_mapping_file", shortName = "sample_rename_mapping_file", doc = "Rename sample IDs on-the-fly at runtime using the provided mapping file", required = false, exclusiveOf = "", validation = "")
  var sample_rename_mapping_file: Option[File] = config("sample_rename_mapping_file")

  /** Enable unsafe operations: nothing will be checked at runtime */
  @Argument(fullName = "unsafe", shortName = "U", doc = "Enable unsafe operations: nothing will be checked at runtime", required = false, exclusiveOf = "", validation = "")
  var unsafe: Option[String] = config("unsafe")

  /** Disable both auto-generation of index files and index file locking */
  @Argument(fullName = "disable_auto_index_creation_and_locking_when_reading_rods", shortName = "disable_auto_index_creation_and_locking_when_reading_rods", doc = "Disable both auto-generation of index files and index file locking", required = false, exclusiveOf = "", validation = "")
  var disable_auto_index_creation_and_locking_when_reading_rods: Boolean = config("disable_auto_index_creation_and_locking_when_reading_rods", default = false)

  /** Don't output the usual VCF header tag with the command line. FOR DEBUGGING PURPOSES ONLY. This option is required in order to pass integration tests. */
  @Argument(fullName = "no_cmdline_in_header", shortName = "no_cmdline_in_header", doc = "Don't output the usual VCF header tag with the command line. FOR DEBUGGING PURPOSES ONLY. This option is required in order to pass integration tests.", required = false, exclusiveOf = "", validation = "")
  var no_cmdline_in_header: Boolean = config("no_cmdline_in_header", default = false)

  /** Just output sites without genotypes (i.e. only the first 8 columns of the VCF) */
  @Argument(fullName = "sites_only", shortName = "sites_only", doc = "Just output sites without genotypes (i.e. only the first 8 columns of the VCF)", required = false, exclusiveOf = "", validation = "")
  var sites_only: Boolean = config("sites_only", default = false)

  /** Always output all the records in VCF FORMAT fields, even if some are missing */
  @Argument(fullName = "never_trim_vcf_format_field", shortName = "writeFullFormat", doc = "Always output all the records in VCF FORMAT fields, even if some are missing", required = false, exclusiveOf = "", validation = "")
  var never_trim_vcf_format_field: Boolean = config("never_trim_vcf_format_field", default = false)

  /** Force BCF output, regardless of the file's extension */
  @Argument(fullName = "bcf", shortName = "bcf", doc = "Force BCF output, regardless of the file's extension", required = false, exclusiveOf = "", validation = "")
  var bcf: Boolean = config("bcf", default = false)

  /** Compression level to use for writing BAM files (0 - 9, higher is more compressed) */
  @Argument(fullName = "bam_compression", shortName = "compress", doc = "Compression level to use for writing BAM files (0 - 9, higher is more compressed)", required = false, exclusiveOf = "", validation = "")
  var bam_compression: Option[Int] = config("bam_compression")

  /** If provided, output BAM/CRAM files will be simplified to include just key reads for downstream variation discovery analyses (removing duplicates, PF-, non-primary reads), as well stripping all extended tags from the kept reads except the read group identifier */
  @Argument(fullName = "simplifyBAM", shortName = "simplifyBAM", doc = "If provided, output BAM/CRAM files will be simplified to include just key reads for downstream variation discovery analyses (removing duplicates, PF-, non-primary reads), as well stripping all extended tags from the kept reads except the read group identifier", required = false, exclusiveOf = "", validation = "")
  var simplifyBAM: Boolean = config("simplifyBAM", default = false)

  /** Turn off on-the-fly creation of indices for output BAM/CRAM files. */
  @Argument(fullName = "disable_bam_indexing", shortName = "", doc = "Turn off on-the-fly creation of indices for output BAM/CRAM files.", required = false, exclusiveOf = "", validation = "")
  var disable_bam_indexing: Boolean = config("disable_bam_indexing", default = false)

  /** Enable on-the-fly creation of md5s for output BAM files. */
  @Argument(fullName = "generate_md5", shortName = "", doc = "Enable on-the-fly creation of md5s for output BAM files.", required = false, exclusiveOf = "", validation = "")
  var generate_md5: Boolean = config("generate_md5", default = false)

  /** Number of data threads to allocate to this analysis */
  @Argument(fullName = "num_threads", shortName = "nt", doc = "Number of data threads to allocate to this analysis", required = false, exclusiveOf = "", validation = "")
  var num_threads: Option[Int] = None

  /** Number of CPU threads to allocate per data thread */
  @Argument(fullName = "num_cpu_threads_per_data_thread", shortName = "nct", doc = "Number of CPU threads to allocate per data thread", required = false, exclusiveOf = "", validation = "")
  var num_cpu_threads_per_data_thread: Option[Int] = None

  /** Number of given threads to allocate to BAM IO */
  @Argument(fullName = "num_io_threads", shortName = "nit", doc = "Number of given threads to allocate to BAM IO", required = false, exclusiveOf = "", validation = "")
  var num_io_threads: Option[Int] = None

  /** Enable threading efficiency monitoring */
  @Argument(fullName = "monitorThreadEfficiency", shortName = "mte", doc = "Enable threading efficiency monitoring", required = false, exclusiveOf = "", validation = "")
  var monitorThreadEfficiency: Boolean = config("monitorThreadEfficiency", default = false)

  /** When using IO threads, total number of BAM file handles to keep open simultaneously */
  @Argument(fullName = "num_bam_file_handles", shortName = "bfh", doc = "When using IO threads, total number of BAM file handles to keep open simultaneously", required = false, exclusiveOf = "", validation = "")
  var num_bam_file_handles: Option[Int] = None

  /** Exclude read groups based on tags */
  @Input(fullName = "read_group_black_list", shortName = "rgbl", doc = "Exclude read groups based on tags", required = false, exclusiveOf = "", validation = "")
  var read_group_black_list: List[File] = config("read_group_black_list", default = Nil)

  /** Pedigree files for samples */
  @Argument(fullName = "pedigree", shortName = "ped", doc = "Pedigree files for samples", required = false, exclusiveOf = "", validation = "")
  var pedigree: List[File] = config("pedigree", default = Nil)

  /** Pedigree string for samples */
  @Argument(fullName = "pedigreeString", shortName = "pedString", doc = "Pedigree string for samples", required = false, exclusiveOf = "", validation = "")
  var pedigreeString: List[String] = config("pedigreeString", default = Nil)

  /** Validation strictness for pedigree information */
  @Argument(fullName = "pedigreeValidationType", shortName = "pedValidationType", doc = "Validation strictness for pedigree information", required = false, exclusiveOf = "", validation = "")
  var pedigreeValidationType: Option[String] = config("pedigreeValidationType")

  /** Allow interval processing with an unsupported BAM/CRAM */
  @Argument(fullName = "allow_intervals_with_unindexed_bam", shortName = "", doc = "Allow interval processing with an unsupported BAM/CRAM", required = false, exclusiveOf = "", validation = "")
  var allow_intervals_with_unindexed_bam: Boolean = config("allow_intervals_with_unindexed_bam", default = false)

  /** Write a BCF copy of the output VCF */
  @Argument(fullName = "generateShadowBCF", shortName = "generateShadowBCF", doc = "Write a BCF copy of the output VCF", required = false, exclusiveOf = "", validation = "")
  var generateShadowBCF: Boolean = config("generateShadowBCF", default = false)

  /** Type of IndexCreator to use for VCF/BCF indices */
  @Argument(fullName = "variant_index_type", shortName = "variant_index_type", doc = "Type of IndexCreator to use for VCF/BCF indices", required = false, exclusiveOf = "", validation = "")
  var variant_index_type: Option[String] = config("variant_index_type")

  /** Parameter to pass to the VCF/BCF IndexCreator */
  @Argument(fullName = "variant_index_parameter", shortName = "variant_index_parameter", doc = "Parameter to pass to the VCF/BCF IndexCreator", required = false, exclusiveOf = "", validation = "")
  var variant_index_parameter: Option[Int] = config("variant_index_parameter")

  /** Reference window stop */
  @Argument(fullName = "reference_window_stop", shortName = "ref_win_stop", doc = "Reference window stop", required = false, exclusiveOf = "", validation = "")
  var reference_window_stop: Option[Int] = config("reference_window_stop")

  /** Set the minimum level of logging */
  @Argument(fullName = "logging_level", shortName = "l", doc = "Set the minimum level of logging", required = false, exclusiveOf = "", validation = "")
  var logging_level: Option[String] = config("logging_level")

  /** Set the logging location */
  @Output(fullName = "log_to_file", shortName = "log", doc = "Set the logging location", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var log_to_file: File = _

  def versionRegex: Regex = """(.*)""".r
  override def versionExitcode = List(0, 1)
  def versionCommand: String = executable + " -jar " + jarFile + " -version"

  override def defaultCoreMemory = 4.0
  override def faiRequired = true
  override def dictRequired = true

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (interval_set_rule != null && interval_set_rule.isEmpty) {
      val v: Option[String] = config("interval_set_rule")
      interval_set_rule = v.map(IntervalSetRule.valueOf)
    }
    if (interval_merging != null && interval_merging.isEmpty) {
      val v: Option[String] = config("interval_merging")
      interval_merging = v.map(IntervalMergingRule.valueOf)
    }
    if (reference_sequence == null) reference_sequence = referenceFasta()
    input_fileIndexes ++= input_file.filter(orig => orig != null && orig.getName.endsWith(".bam")).flatMap(orig => Array(new File(orig.getPath.stripSuffix(".bam") + ".bai")))
    if (num_threads.isDefined) nCoresRequest = num_threads
    if (num_cpu_threads_per_data_thread.isDefined) nCoresRequest = Some(nCoresRequest.getOrElse(1) * num_cpu_threads_per_data_thread.getOrElse(1))
  }

  override def cmdLine: String = super.cmdLine +
    required("-T", analysis_type) +
    repeat("-I", input_file, formatPrefix = TaggedFile.formatCommandLineParameter) +
    conditional(showFullBamList, "--showFullBamList") +
    optional("-rbs", read_buffer_size) +
    optional("-et", phone_home) +
    optional("-K", gatk_key) +
    optional("-tag", tag) +
    repeat("-rf", read_filter) +
    repeat("-drf", disable_read_filter) +
    repeat("-L", intervals) +
    repeat("-L", intervalsString) +
    repeat("-XL", excludeIntervals) +
    repeat("-XL", excludeIntervalsString) +
    optional("-isr", interval_set_rule) +
    optional("-im", interval_merging) +
    optional("-ip", interval_padding) +
    optional("-R", reference_sequence) +
    conditional(nonDeterministicRandomSeed, "-ndrs") +
    conditional(disableDithering, "--disableDithering") +
    optional("-maxRuntime", maxRuntime) +
    optional("-maxRuntimeUnits", maxRuntimeUnits) +
    optional("-dt", downsampling_type) +
    optional("-dfrac", downsample_to_fraction, format = downsample_to_fractionFormat) +
    optional("-dcov", downsample_to_coverage) +
    optional("-baq", baq) +
    optional("-baqGOP", baqGapOpenPenalty, format = baqGapOpenPenaltyFormat) +
    conditional(refactor_NDN_cigar_string, "-fixNDN") +
    conditional(fix_misencoded_quality_scores, "-fixMisencodedQuals") +
    conditional(allow_potentially_misencoded_quality_scores, "-allowPotentiallyMisencodedQuals") +
    conditional(useOriginalQualities, "-OQ") +
    optional("-DBQ", defaultBaseQualities) +
    optional("-PF", performanceLog) +
    optional("-BQSR", BQSR) +
    optional("-qq", quantize_quals) +
    repeat("-SQQ", static_quantized_quals) +
    conditional(round_down_quantized, "-RDQ") +
    conditional(disable_indel_quals, "-DIQ") +
    conditional(emit_original_quals, "-EOQ") +
    optional("-preserveQ", preserve_qscores_less_than) +
    optional("-globalQScorePrior", globalQScorePrior, format = globalQScorePriorFormat) +
    optional("-S", validation_strictness) +
    conditional(remove_program_records, "-rpr") +
    conditional(keep_program_records, "-kpr") +
    optional("-sample_rename_mapping_file", sample_rename_mapping_file) +
    optional("-U", unsafe) +
    conditional(disable_auto_index_creation_and_locking_when_reading_rods, "-disable_auto_index_creation_and_locking_when_reading_rods") +
    conditional(no_cmdline_in_header, "-no_cmdline_in_header") +
    conditional(sites_only, "-sites_only") +
    conditional(never_trim_vcf_format_field, "-writeFullFormat") +
    conditional(bcf, "-bcf") +
    optional("-compress", bam_compression) +
    conditional(simplifyBAM, "-simplifyBAM") +
    conditional(disable_bam_indexing, "--disable_bam_indexing") +
    conditional(generate_md5, "--generate_md5") +
    optional("-nt", num_threads) +
    optional("-nct", num_cpu_threads_per_data_thread) +
    optional("-nit", num_io_threads) +
    conditional(monitorThreadEfficiency, "-mte") +
    optional("-bfh", num_bam_file_handles) +
    repeat("-rgbl", read_group_black_list) +
    repeat("-ped", pedigree) +
    repeat("-pedString", pedigreeString) +
    optional("-pedValidationType", pedigreeValidationType) +
    conditional(allow_intervals_with_unindexed_bam, "--allow_intervals_with_unindexed_bam") +
    conditional(generateShadowBCF, "-generateShadowBCF") +
    optional("-variant_index_type", variant_index_type) +
    optional("-variant_index_parameter", variant_index_parameter) +
    optional("-ref_win_stop", reference_window_stop) +
    optional("-l", logging_level) +
    optional("-log", log_to_file)
}
