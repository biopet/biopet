package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{ Gather, Input, Output, Argument }

trait CommandLineGATK extends BiopetJavaCommandLineFunction {
  analysisName = "CommandLineGATK"
  javaMainClass = "org.broadinstitute.gatk.engine.CommandLineGATK"

  /** Name of the tool to run */
  @Argument(fullName = "analysis_type", shortName = "T", doc = "Name of the tool to run", required = true, exclusiveOf = "", validation = "")
  var analysis_type: String = _

  /**
   * Short name of analysis_type
   * @return Short name of analysis_type
   */
  def T = this.analysis_type

  /**
   * Short name of analysis_type
   * @param value Short name of analysis_type
   */
  def T_=(value: String) { this.analysis_type = value }

  /** Input file containing sequence data (BAM or CRAM) */
  @Input(fullName = "input_file", shortName = "I", doc = "Input file containing sequence data (BAM or CRAM)", required = false, exclusiveOf = "", validation = "")
  var input_file: Seq[File] = Nil

  /**
   * Short name of input_file
   * @return Short name of input_file
   */
  def I = this.input_file

  /**
   * Short name of input_file
   * @param value Short name of input_file
   */
  def I_=(value: Seq[File]) { this.input_file = value }

  /** Dependencies on any indexes of input_file */
  @Input(fullName = "input_fileIndexes", shortName = "", doc = "Dependencies on any indexes of input_file", required = false, exclusiveOf = "", validation = "")
  private var input_fileIndexes: Seq[File] = Nil

  /** Emit a log entry (level INFO) containing the full list of sequence data files to be included in the analysis (including files inside .bam.list or .cram.list files). */
  @Argument(fullName = "showFullBamList", shortName = "", doc = "Emit a log entry (level INFO) containing the full list of sequence data files to be included in the analysis (including files inside .bam.list or .cram.list files).", required = false, exclusiveOf = "", validation = "")
  var showFullBamList: Boolean = _

  /** Number of reads per SAM file to buffer in memory */
  @Argument(fullName = "read_buffer_size", shortName = "rbs", doc = "Number of reads per SAM file to buffer in memory", required = false, exclusiveOf = "", validation = "")
  var read_buffer_size: Option[Int] = None

  /**
   * Short name of read_buffer_size
   * @return Short name of read_buffer_size
   */
  def rbs = this.read_buffer_size

  /**
   * Short name of read_buffer_size
   * @param value Short name of read_buffer_size
   */
  def rbs_=(value: Option[Int]) { this.read_buffer_size = value }

  /** Run reporting mode */
  @Argument(fullName = "phone_home", shortName = "et", doc = "Run reporting mode", required = false, exclusiveOf = "", validation = "")
  var phone_home: org.broadinstitute.gatk.engine.phonehome.GATKRunReport.PhoneHomeOption = _

  /**
   * Short name of phone_home
   * @return Short name of phone_home
   */
  def et = this.phone_home

  /**
   * Short name of phone_home
   * @param value Short name of phone_home
   */
  def et_=(value: org.broadinstitute.gatk.engine.phonehome.GATKRunReport.PhoneHomeOption) { this.phone_home = value }

  /** GATK key file required to run with -et NO_ET */
  @Argument(fullName = "gatk_key", shortName = "K", doc = "GATK key file required to run with -et NO_ET", required = false, exclusiveOf = "", validation = "")
  var gatk_key: File = _

  /**
   * Short name of gatk_key
   * @return Short name of gatk_key
   */
  def K = this.gatk_key

  /**
   * Short name of gatk_key
   * @param value Short name of gatk_key
   */
  def K_=(value: File) { this.gatk_key = value }

  /** Tag to identify this GATK run as part of a group of runs */
  @Argument(fullName = "tag", shortName = "tag", doc = "Tag to identify this GATK run as part of a group of runs", required = false, exclusiveOf = "", validation = "")
  var tag: String = _

  /** Filters to apply to reads before analysis */
  @Argument(fullName = "read_filter", shortName = "rf", doc = "Filters to apply to reads before analysis", required = false, exclusiveOf = "", validation = "")
  var read_filter: Seq[String] = Nil

  /**
   * Short name of read_filter
   * @return Short name of read_filter
   */
  def rf = this.read_filter

  /**
   * Short name of read_filter
   * @param value Short name of read_filter
   */
  def rf_=(value: Seq[String]) { this.read_filter = value }

  /** Read filters to disable */
  @Argument(fullName = "disable_read_filter", shortName = "drf", doc = "Read filters to disable", required = false, exclusiveOf = "", validation = "")
  var disable_read_filter: Seq[String] = Nil

  /**
   * Short name of disable_read_filter
   * @return Short name of disable_read_filter
   */
  def drf = this.disable_read_filter

  /**
   * Short name of disable_read_filter
   * @param value Short name of disable_read_filter
   */
  def drf_=(value: Seq[String]) { this.disable_read_filter = value }

  /** One or more genomic intervals over which to operate */
  @Input(fullName = "intervals", shortName = "L", doc = "One or more genomic intervals over which to operate", required = false, exclusiveOf = "intervalsString", validation = "")
  var intervals: Seq[File] = Nil

  /**
   * Short name of intervals
   * @return Short name of intervals
   */
  def L = this.intervals

  /**
   * Short name of intervals
   * @param value Short name of intervals
   */
  def L_=(value: Seq[File]) { this.intervals = value }

  /** One or more genomic intervals over which to operate */
  @Argument(fullName = "intervalsString", shortName = "L", doc = "One or more genomic intervals over which to operate", required = false, exclusiveOf = "intervals", validation = "")
  var intervalsString: Seq[String] = Nil

  /**
   * Short name of intervalsString
   * @return Short name of intervalsString
   */
  def LString = this.intervalsString

  /**
   * Short name of intervalsString
   * @param value Short name of intervalsString
   */
  def LString_=(value: Seq[String]) { this.intervalsString = value }

  /** One or more genomic intervals to exclude from processing */
  @Input(fullName = "excludeIntervals", shortName = "XL", doc = "One or more genomic intervals to exclude from processing", required = false, exclusiveOf = "excludeIntervalsString", validation = "")
  var excludeIntervals: Seq[File] = Nil

  /**
   * Short name of excludeIntervals
   * @return Short name of excludeIntervals
   */
  def XL = this.excludeIntervals

  /**
   * Short name of excludeIntervals
   * @param value Short name of excludeIntervals
   */
  def XL_=(value: Seq[File]) { this.excludeIntervals = value }

  /** One or more genomic intervals to exclude from processing */
  @Argument(fullName = "excludeIntervalsString", shortName = "XL", doc = "One or more genomic intervals to exclude from processing", required = false, exclusiveOf = "excludeIntervals", validation = "")
  var excludeIntervalsString: Seq[String] = Nil

  /**
   * Short name of excludeIntervalsString
   * @return Short name of excludeIntervalsString
   */
  def XLString = this.excludeIntervalsString

  /**
   * Short name of excludeIntervalsString
   * @param value Short name of excludeIntervalsString
   */
  def XLString_=(value: Seq[String]) { this.excludeIntervalsString = value }

  /** Set merging approach to use for combining interval inputs */
  @Argument(fullName = "interval_set_rule", shortName = "isr", doc = "Set merging approach to use for combining interval inputs", required = false, exclusiveOf = "", validation = "")
  var interval_set_rule: org.broadinstitute.gatk.utils.interval.IntervalSetRule = _

  /**
   * Short name of interval_set_rule
   * @return Short name of interval_set_rule
   */
  def isr = this.interval_set_rule

  /**
   * Short name of interval_set_rule
   * @param value Short name of interval_set_rule
   */
  def isr_=(value: org.broadinstitute.gatk.utils.interval.IntervalSetRule) { this.interval_set_rule = value }

  /** Interval merging rule for abutting intervals */
  @Argument(fullName = "interval_merging", shortName = "im", doc = "Interval merging rule for abutting intervals", required = false, exclusiveOf = "", validation = "")
  var interval_merging: org.broadinstitute.gatk.utils.interval.IntervalMergingRule = _

  /**
   * Short name of interval_merging
   * @return Short name of interval_merging
   */
  def im = this.interval_merging

  /**
   * Short name of interval_merging
   * @param value Short name of interval_merging
   */
  def im_=(value: org.broadinstitute.gatk.utils.interval.IntervalMergingRule) { this.interval_merging = value }

  /** Amount of padding (in bp) to add to each interval */
  @Argument(fullName = "interval_padding", shortName = "ip", doc = "Amount of padding (in bp) to add to each interval", required = false, exclusiveOf = "", validation = "")
  var interval_padding: Option[Int] = None

  /**
   * Short name of interval_padding
   * @return Short name of interval_padding
   */
  def ip = this.interval_padding

  /**
   * Short name of interval_padding
   * @param value Short name of interval_padding
   */
  def ip_=(value: Option[Int]) { this.interval_padding = value }

  /** Reference sequence file */
  @Input(fullName = "reference_sequence", shortName = "R", doc = "Reference sequence file", required = false, exclusiveOf = "", validation = "")
  var reference_sequence: File = _

  /**
   * Short name of reference_sequence
   * @return Short name of reference_sequence
   */
  def R = this.reference_sequence

  /**
   * Short name of reference_sequence
   * @param value Short name of reference_sequence
   */
  def R_=(value: File) { this.reference_sequence = value }

  /** Use a non-deterministic random seed */
  @Argument(fullName = "nonDeterministicRandomSeed", shortName = "ndrs", doc = "Use a non-deterministic random seed", required = false, exclusiveOf = "", validation = "")
  var nonDeterministicRandomSeed: Boolean = _

  /**
   * Short name of nonDeterministicRandomSeed
   * @return Short name of nonDeterministicRandomSeed
   */
  def ndrs = this.nonDeterministicRandomSeed

  /**
   * Short name of nonDeterministicRandomSeed
   * @param value Short name of nonDeterministicRandomSeed
   */
  def ndrs_=(value: Boolean) { this.nonDeterministicRandomSeed = value }

  /** Completely eliminates randomized dithering from rank sum tests. */
  @Argument(fullName = "disableDithering", shortName = "", doc = "Completely eliminates randomized dithering from rank sum tests.", required = false, exclusiveOf = "", validation = "")
  var disableDithering: Boolean = _

  /** Stop execution cleanly as soon as maxRuntime has been reached */
  @Argument(fullName = "maxRuntime", shortName = "maxRuntime", doc = "Stop execution cleanly as soon as maxRuntime has been reached", required = false, exclusiveOf = "", validation = "")
  var maxRuntime: Option[Long] = None

  /** Unit of time used by maxRuntime */
  @Argument(fullName = "maxRuntimeUnits", shortName = "maxRuntimeUnits", doc = "Unit of time used by maxRuntime", required = false, exclusiveOf = "", validation = "")
  var maxRuntimeUnits: java.util.concurrent.TimeUnit = _

  /** Type of read downsampling to employ at a given locus */
  @Argument(fullName = "downsampling_type", shortName = "dt", doc = "Type of read downsampling to employ at a given locus", required = false, exclusiveOf = "", validation = "")
  var downsampling_type: org.broadinstitute.gatk.utils.downsampling.DownsampleType = _

  /**
   * Short name of downsampling_type
   * @return Short name of downsampling_type
   */
  def dt = this.downsampling_type

  /**
   * Short name of downsampling_type
   * @param value Short name of downsampling_type
   */
  def dt_=(value: org.broadinstitute.gatk.utils.downsampling.DownsampleType) { this.downsampling_type = value }

  /** Fraction of reads to downsample to */
  @Argument(fullName = "downsample_to_fraction", shortName = "dfrac", doc = "Fraction of reads to downsample to", required = false, exclusiveOf = "", validation = "")
  var downsample_to_fraction: Option[Double] = None

  /**
   * Short name of downsample_to_fraction
   * @return Short name of downsample_to_fraction
   */
  def dfrac = this.downsample_to_fraction

  /**
   * Short name of downsample_to_fraction
   * @param value Short name of downsample_to_fraction
   */
  def dfrac_=(value: Option[Double]) { this.downsample_to_fraction = value }

  /** Format string for downsample_to_fraction */
  @Argument(fullName = "downsample_to_fractionFormat", shortName = "", doc = "Format string for downsample_to_fraction", required = false, exclusiveOf = "", validation = "")
  var downsample_to_fractionFormat: String = "%s"

  /** Target coverage threshold for downsampling to coverage */
  @Argument(fullName = "downsample_to_coverage", shortName = "dcov", doc = "Target coverage threshold for downsampling to coverage", required = false, exclusiveOf = "", validation = "")
  var downsample_to_coverage: Option[Int] = None

  /**
   * Short name of downsample_to_coverage
   * @return Short name of downsample_to_coverage
   */
  def dcov = this.downsample_to_coverage

  /**
   * Short name of downsample_to_coverage
   * @param value Short name of downsample_to_coverage
   */
  def dcov_=(value: Option[Int]) { this.downsample_to_coverage = value }

  /** Type of BAQ calculation to apply in the engine */
  @Argument(fullName = "baq", shortName = "baq", doc = "Type of BAQ calculation to apply in the engine", required = false, exclusiveOf = "", validation = "")
  var baq: org.broadinstitute.gatk.utils.baq.BAQ.CalculationMode = _

  /** BAQ gap open penalty */
  @Argument(fullName = "baqGapOpenPenalty", shortName = "baqGOP", doc = "BAQ gap open penalty", required = false, exclusiveOf = "", validation = "")
  var baqGapOpenPenalty: Option[Double] = None

  /**
   * Short name of baqGapOpenPenalty
   * @return Short name of baqGapOpenPenalty
   */
  def baqGOP = this.baqGapOpenPenalty

  /**
   * Short name of baqGapOpenPenalty
   * @param value Short name of baqGapOpenPenalty
   */
  def baqGOP_=(value: Option[Double]) { this.baqGapOpenPenalty = value }

  /** Format string for baqGapOpenPenalty */
  @Argument(fullName = "baqGapOpenPenaltyFormat", shortName = "", doc = "Format string for baqGapOpenPenalty", required = false, exclusiveOf = "", validation = "")
  var baqGapOpenPenaltyFormat: String = "%s"

  /** Reduce NDN elements in CIGAR string */
  @Argument(fullName = "refactor_NDN_cigar_string", shortName = "fixNDN", doc = "Reduce NDN elements in CIGAR string", required = false, exclusiveOf = "", validation = "")
  var refactor_NDN_cigar_string: Boolean = _

  /**
   * Short name of refactor_NDN_cigar_string
   * @return Short name of refactor_NDN_cigar_string
   */
  def fixNDN = this.refactor_NDN_cigar_string

  /**
   * Short name of refactor_NDN_cigar_string
   * @param value Short name of refactor_NDN_cigar_string
   */
  def fixNDN_=(value: Boolean) { this.refactor_NDN_cigar_string = value }

  /** Fix mis-encoded base quality scores */
  @Argument(fullName = "fix_misencoded_quality_scores", shortName = "fixMisencodedQuals", doc = "Fix mis-encoded base quality scores", required = false, exclusiveOf = "", validation = "")
  var fix_misencoded_quality_scores: Boolean = _

  /**
   * Short name of fix_misencoded_quality_scores
   * @return Short name of fix_misencoded_quality_scores
   */
  def fixMisencodedQuals = this.fix_misencoded_quality_scores

  /**
   * Short name of fix_misencoded_quality_scores
   * @param value Short name of fix_misencoded_quality_scores
   */
  def fixMisencodedQuals_=(value: Boolean) { this.fix_misencoded_quality_scores = value }

  /** Ignore warnings about base quality score encoding */
  @Argument(fullName = "allow_potentially_misencoded_quality_scores", shortName = "allowPotentiallyMisencodedQuals", doc = "Ignore warnings about base quality score encoding", required = false, exclusiveOf = "", validation = "")
  var allow_potentially_misencoded_quality_scores: Boolean = _

  /**
   * Short name of allow_potentially_misencoded_quality_scores
   * @return Short name of allow_potentially_misencoded_quality_scores
   */
  def allowPotentiallyMisencodedQuals = this.allow_potentially_misencoded_quality_scores

  /**
   * Short name of allow_potentially_misencoded_quality_scores
   * @param value Short name of allow_potentially_misencoded_quality_scores
   */
  def allowPotentiallyMisencodedQuals_=(value: Boolean) { this.allow_potentially_misencoded_quality_scores = value }

  /** Use the base quality scores from the OQ tag */
  @Argument(fullName = "useOriginalQualities", shortName = "OQ", doc = "Use the base quality scores from the OQ tag", required = false, exclusiveOf = "", validation = "")
  var useOriginalQualities: Boolean = _

  /**
   * Short name of useOriginalQualities
   * @return Short name of useOriginalQualities
   */
  def OQ = this.useOriginalQualities

  /**
   * Short name of useOriginalQualities
   * @param value Short name of useOriginalQualities
   */
  def OQ_=(value: Boolean) { this.useOriginalQualities = value }

  /** Assign a default base quality */
  @Argument(fullName = "defaultBaseQualities", shortName = "DBQ", doc = "Assign a default base quality", required = false, exclusiveOf = "", validation = "")
  var defaultBaseQualities: Option[Byte] = None

  /**
   * Short name of defaultBaseQualities
   * @return Short name of defaultBaseQualities
   */
  def DBQ = this.defaultBaseQualities

  /**
   * Short name of defaultBaseQualities
   * @param value Short name of defaultBaseQualities
   */
  def DBQ_=(value: Option[Byte]) { this.defaultBaseQualities = value }

  /** Write GATK runtime performance log to this file */
  @Argument(fullName = "performanceLog", shortName = "PF", doc = "Write GATK runtime performance log to this file", required = false, exclusiveOf = "", validation = "")
  var performanceLog: File = _

  /**
   * Short name of performanceLog
   * @return Short name of performanceLog
   */
  def PF = this.performanceLog

  /**
   * Short name of performanceLog
   * @param value Short name of performanceLog
   */
  def PF_=(value: File) { this.performanceLog = value }

  /** Input covariates table file for on-the-fly base quality score recalibration */
  @Input(fullName = "BQSR", shortName = "BQSR", doc = "Input covariates table file for on-the-fly base quality score recalibration", required = false, exclusiveOf = "", validation = "")
  var BQSR: File = _

  /** Quantize quality scores to a given number of levels (with -BQSR) */
  @Argument(fullName = "quantize_quals", shortName = "qq", doc = "Quantize quality scores to a given number of levels (with -BQSR)", required = false, exclusiveOf = "", validation = "")
  var quantize_quals: Option[Int] = None

  /**
   * Short name of quantize_quals
   * @return Short name of quantize_quals
   */
  def qq = this.quantize_quals

  /**
   * Short name of quantize_quals
   * @param value Short name of quantize_quals
   */
  def qq_=(value: Option[Int]) { this.quantize_quals = value }

  /** Use static quantized quality scores to a given number of levels (with -BQSR) */
  @Argument(fullName = "static_quantized_quals", shortName = "SQQ", doc = "Use static quantized quality scores to a given number of levels (with -BQSR)", required = false, exclusiveOf = "quantize_quals", validation = "")
  var static_quantized_quals: Seq[Int] = Nil

  /**
   * Short name of static_quantized_quals
   * @return Short name of static_quantized_quals
   */
  def SQQ = this.static_quantized_quals

  /**
   * Short name of static_quantized_quals
   * @param value Short name of static_quantized_quals
   */
  def SQQ_=(value: Seq[Int]) { this.static_quantized_quals = value }

  /** Round quals down to nearest quantized qual */
  @Argument(fullName = "round_down_quantized", shortName = "RDQ", doc = "Round quals down to nearest quantized qual", required = false, exclusiveOf = "quantize_quals", validation = "")
  var round_down_quantized: Boolean = _

  /**
   * Short name of round_down_quantized
   * @return Short name of round_down_quantized
   */
  def RDQ = this.round_down_quantized

  /**
   * Short name of round_down_quantized
   * @param value Short name of round_down_quantized
   */
  def RDQ_=(value: Boolean) { this.round_down_quantized = value }

  /** Disable printing of base insertion and deletion tags (with -BQSR) */
  @Argument(fullName = "disable_indel_quals", shortName = "DIQ", doc = "Disable printing of base insertion and deletion tags (with -BQSR)", required = false, exclusiveOf = "", validation = "")
  var disable_indel_quals: Boolean = _

  /**
   * Short name of disable_indel_quals
   * @return Short name of disable_indel_quals
   */
  def DIQ = this.disable_indel_quals

  /**
   * Short name of disable_indel_quals
   * @param value Short name of disable_indel_quals
   */
  def DIQ_=(value: Boolean) { this.disable_indel_quals = value }

  /** Emit the OQ tag with the original base qualities (with -BQSR) */
  @Argument(fullName = "emit_original_quals", shortName = "EOQ", doc = "Emit the OQ tag with the original base qualities (with -BQSR)", required = false, exclusiveOf = "", validation = "")
  var emit_original_quals: Boolean = _

  /**
   * Short name of emit_original_quals
   * @return Short name of emit_original_quals
   */
  def EOQ = this.emit_original_quals

  /**
   * Short name of emit_original_quals
   * @param value Short name of emit_original_quals
   */
  def EOQ_=(value: Boolean) { this.emit_original_quals = value }

  /** Don't recalibrate bases with quality scores less than this threshold (with -BQSR) */
  @Argument(fullName = "preserve_qscores_less_than", shortName = "preserveQ", doc = "Don't recalibrate bases with quality scores less than this threshold (with -BQSR)", required = false, exclusiveOf = "", validation = "")
  var preserve_qscores_less_than: Option[Int] = None

  /**
   * Short name of preserve_qscores_less_than
   * @return Short name of preserve_qscores_less_than
   */
  def preserveQ = this.preserve_qscores_less_than

  /**
   * Short name of preserve_qscores_less_than
   * @param value Short name of preserve_qscores_less_than
   */
  def preserveQ_=(value: Option[Int]) { this.preserve_qscores_less_than = value }

  /** Global Qscore Bayesian prior to use for BQSR */
  @Argument(fullName = "globalQScorePrior", shortName = "globalQScorePrior", doc = "Global Qscore Bayesian prior to use for BQSR", required = false, exclusiveOf = "", validation = "")
  var globalQScorePrior: Option[Double] = None

  /** Format string for globalQScorePrior */
  @Argument(fullName = "globalQScorePriorFormat", shortName = "", doc = "Format string for globalQScorePrior", required = false, exclusiveOf = "", validation = "")
  var globalQScorePriorFormat: String = "%s"

  /** How strict should we be with validation */
  @Argument(fullName = "validation_strictness", shortName = "S", doc = "How strict should we be with validation", required = false, exclusiveOf = "", validation = "")
  var validation_strictness: htsjdk.samtools.ValidationStringency = _

  /**
   * Short name of validation_strictness
   * @return Short name of validation_strictness
   */
  def S = this.validation_strictness

  /**
   * Short name of validation_strictness
   * @param value Short name of validation_strictness
   */
  def S_=(value: htsjdk.samtools.ValidationStringency) { this.validation_strictness = value }

  /** Remove program records from the SAM header */
  @Argument(fullName = "remove_program_records", shortName = "rpr", doc = "Remove program records from the SAM header", required = false, exclusiveOf = "", validation = "")
  var remove_program_records: Boolean = _

  /**
   * Short name of remove_program_records
   * @return Short name of remove_program_records
   */
  def rpr = this.remove_program_records

  /**
   * Short name of remove_program_records
   * @param value Short name of remove_program_records
   */
  def rpr_=(value: Boolean) { this.remove_program_records = value }

  /** Keep program records in the SAM header */
  @Argument(fullName = "keep_program_records", shortName = "kpr", doc = "Keep program records in the SAM header", required = false, exclusiveOf = "", validation = "")
  var keep_program_records: Boolean = _

  /**
   * Short name of keep_program_records
   * @return Short name of keep_program_records
   */
  def kpr = this.keep_program_records

  /**
   * Short name of keep_program_records
   * @param value Short name of keep_program_records
   */
  def kpr_=(value: Boolean) { this.keep_program_records = value }

  /** Rename sample IDs on-the-fly at runtime using the provided mapping file */
  @Argument(fullName = "sample_rename_mapping_file", shortName = "sample_rename_mapping_file", doc = "Rename sample IDs on-the-fly at runtime using the provided mapping file", required = false, exclusiveOf = "", validation = "")
  var sample_rename_mapping_file: File = _

  /** Enable unsafe operations: nothing will be checked at runtime */
  @Argument(fullName = "unsafe", shortName = "U", doc = "Enable unsafe operations: nothing will be checked at runtime", required = false, exclusiveOf = "", validation = "")
  var unsafe: org.broadinstitute.gatk.utils.ValidationExclusion.TYPE = _

  /**
   * Short name of unsafe
   * @return Short name of unsafe
   */
  def U = this.unsafe

  /**
   * Short name of unsafe
   * @param value Short name of unsafe
   */
  def U_=(value: org.broadinstitute.gatk.utils.ValidationExclusion.TYPE) { this.unsafe = value }

  /** Disable both auto-generation of index files and index file locking */
  @Argument(fullName = "disable_auto_index_creation_and_locking_when_reading_rods", shortName = "disable_auto_index_creation_and_locking_when_reading_rods", doc = "Disable both auto-generation of index files and index file locking", required = false, exclusiveOf = "", validation = "")
  var disable_auto_index_creation_and_locking_when_reading_rods: Boolean = _

  /** Don't output the usual VCF header tag with the command line. FOR DEBUGGING PURPOSES ONLY. This option is required in order to pass integration tests. */
  @Argument(fullName = "no_cmdline_in_header", shortName = "no_cmdline_in_header", doc = "Don't output the usual VCF header tag with the command line. FOR DEBUGGING PURPOSES ONLY. This option is required in order to pass integration tests.", required = false, exclusiveOf = "", validation = "")
  var no_cmdline_in_header: Boolean = _

  /** Just output sites without genotypes (i.e. only the first 8 columns of the VCF) */
  @Argument(fullName = "sites_only", shortName = "sites_only", doc = "Just output sites without genotypes (i.e. only the first 8 columns of the VCF)", required = false, exclusiveOf = "", validation = "")
  var sites_only: Boolean = _

  /** Always output all the records in VCF FORMAT fields, even if some are missing */
  @Argument(fullName = "never_trim_vcf_format_field", shortName = "writeFullFormat", doc = "Always output all the records in VCF FORMAT fields, even if some are missing", required = false, exclusiveOf = "", validation = "")
  var never_trim_vcf_format_field: Boolean = _

  /**
   * Short name of never_trim_vcf_format_field
   * @return Short name of never_trim_vcf_format_field
   */
  def writeFullFormat = this.never_trim_vcf_format_field

  /**
   * Short name of never_trim_vcf_format_field
   * @param value Short name of never_trim_vcf_format_field
   */
  def writeFullFormat_=(value: Boolean) { this.never_trim_vcf_format_field = value }

  /** Force BCF output, regardless of the file's extension */
  @Argument(fullName = "bcf", shortName = "bcf", doc = "Force BCF output, regardless of the file's extension", required = false, exclusiveOf = "", validation = "")
  var bcf: Boolean = _

  /** Compression level to use for writing BAM files (0 - 9, higher is more compressed) */
  @Argument(fullName = "bam_compression", shortName = "compress", doc = "Compression level to use for writing BAM files (0 - 9, higher is more compressed)", required = false, exclusiveOf = "", validation = "")
  var bam_compression: Option[Int] = None

  /**
   * Short name of bam_compression
   * @return Short name of bam_compression
   */
  def compress = this.bam_compression

  /**
   * Short name of bam_compression
   * @param value Short name of bam_compression
   */
  def compress_=(value: Option[Int]) { this.bam_compression = value }

  /** If provided, output BAM/CRAM files will be simplified to include just key reads for downstream variation discovery analyses (removing duplicates, PF-, non-primary reads), as well stripping all extended tags from the kept reads except the read group identifier */
  @Argument(fullName = "simplifyBAM", shortName = "simplifyBAM", doc = "If provided, output BAM/CRAM files will be simplified to include just key reads for downstream variation discovery analyses (removing duplicates, PF-, non-primary reads), as well stripping all extended tags from the kept reads except the read group identifier", required = false, exclusiveOf = "", validation = "")
  var simplifyBAM: Boolean = _

  /** Turn off on-the-fly creation of indices for output BAM/CRAM files. */
  @Argument(fullName = "disable_bam_indexing", shortName = "", doc = "Turn off on-the-fly creation of indices for output BAM/CRAM files.", required = false, exclusiveOf = "", validation = "")
  var disable_bam_indexing: Boolean = _

  /** Enable on-the-fly creation of md5s for output BAM files. */
  @Argument(fullName = "generate_md5", shortName = "", doc = "Enable on-the-fly creation of md5s for output BAM files.", required = false, exclusiveOf = "", validation = "")
  var generate_md5: Boolean = _

  /** Number of data threads to allocate to this analysis */
  @Argument(fullName = "num_threads", shortName = "nt", doc = "Number of data threads to allocate to this analysis", required = false, exclusiveOf = "", validation = "")
  var num_threads: Option[Int] = None

  /**
   * Short name of num_threads
   * @return Short name of num_threads
   */
  def nt = this.num_threads

  /**
   * Short name of num_threads
   * @param value Short name of num_threads
   */
  def nt_=(value: Option[Int]) { this.num_threads = value }

  /** Number of CPU threads to allocate per data thread */
  @Argument(fullName = "num_cpu_threads_per_data_thread", shortName = "nct", doc = "Number of CPU threads to allocate per data thread", required = false, exclusiveOf = "", validation = "")
  var num_cpu_threads_per_data_thread: Option[Int] = None

  /**
   * Short name of num_cpu_threads_per_data_thread
   * @return Short name of num_cpu_threads_per_data_thread
   */
  def nct = this.num_cpu_threads_per_data_thread

  /**
   * Short name of num_cpu_threads_per_data_thread
   * @param value Short name of num_cpu_threads_per_data_thread
   */
  def nct_=(value: Option[Int]) { this.num_cpu_threads_per_data_thread = value }

  /** Number of given threads to allocate to BAM IO */
  @Argument(fullName = "num_io_threads", shortName = "nit", doc = "Number of given threads to allocate to BAM IO", required = false, exclusiveOf = "", validation = "")
  var num_io_threads: Option[Int] = None

  /**
   * Short name of num_io_threads
   * @return Short name of num_io_threads
   */
  def nit = this.num_io_threads

  /**
   * Short name of num_io_threads
   * @param value Short name of num_io_threads
   */
  def nit_=(value: Option[Int]) { this.num_io_threads = value }

  /** Enable threading efficiency monitoring */
  @Argument(fullName = "monitorThreadEfficiency", shortName = "mte", doc = "Enable threading efficiency monitoring", required = false, exclusiveOf = "", validation = "")
  var monitorThreadEfficiency: Boolean = _

  /**
   * Short name of monitorThreadEfficiency
   * @return Short name of monitorThreadEfficiency
   */
  def mte = this.monitorThreadEfficiency

  /**
   * Short name of monitorThreadEfficiency
   * @param value Short name of monitorThreadEfficiency
   */
  def mte_=(value: Boolean) { this.monitorThreadEfficiency = value }

  /** When using IO threads, total number of BAM file handles to keep open simultaneously */
  @Argument(fullName = "num_bam_file_handles", shortName = "bfh", doc = "When using IO threads, total number of BAM file handles to keep open simultaneously", required = false, exclusiveOf = "", validation = "")
  var num_bam_file_handles: Option[Int] = None

  /**
   * Short name of num_bam_file_handles
   * @return Short name of num_bam_file_handles
   */
  def bfh = this.num_bam_file_handles

  /**
   * Short name of num_bam_file_handles
   * @param value Short name of num_bam_file_handles
   */
  def bfh_=(value: Option[Int]) { this.num_bam_file_handles = value }

  /** Exclude read groups based on tags */
  @Input(fullName = "read_group_black_list", shortName = "rgbl", doc = "Exclude read groups based on tags", required = false, exclusiveOf = "", validation = "")
  var read_group_black_list: Seq[File] = Nil

  /**
   * Short name of read_group_black_list
   * @return Short name of read_group_black_list
   */
  def rgbl = this.read_group_black_list

  /**
   * Short name of read_group_black_list
   * @param value Short name of read_group_black_list
   */
  def rgbl_=(value: Seq[File]) { this.read_group_black_list = value }

  /** Pedigree files for samples */
  @Argument(fullName = "pedigree", shortName = "ped", doc = "Pedigree files for samples", required = false, exclusiveOf = "", validation = "")
  var pedigree: Seq[File] = Nil

  /**
   * Short name of pedigree
   * @return Short name of pedigree
   */
  def ped = this.pedigree

  /**
   * Short name of pedigree
   * @param value Short name of pedigree
   */
  def ped_=(value: Seq[File]) { this.pedigree = value }

  /** Pedigree string for samples */
  @Argument(fullName = "pedigreeString", shortName = "pedString", doc = "Pedigree string for samples", required = false, exclusiveOf = "", validation = "")
  var pedigreeString: Seq[String] = Nil

  /**
   * Short name of pedigreeString
   * @return Short name of pedigreeString
   */
  def pedString = this.pedigreeString

  /**
   * Short name of pedigreeString
   * @param value Short name of pedigreeString
   */
  def pedString_=(value: Seq[String]) { this.pedigreeString = value }

  /** Validation strictness for pedigree information */
  @Argument(fullName = "pedigreeValidationType", shortName = "pedValidationType", doc = "Validation strictness for pedigree information", required = false, exclusiveOf = "", validation = "")
  var pedigreeValidationType: org.broadinstitute.gatk.engine.samples.PedigreeValidationType = _

  /**
   * Short name of pedigreeValidationType
   * @return Short name of pedigreeValidationType
   */
  def pedValidationType = this.pedigreeValidationType

  /**
   * Short name of pedigreeValidationType
   * @param value Short name of pedigreeValidationType
   */
  def pedValidationType_=(value: org.broadinstitute.gatk.engine.samples.PedigreeValidationType) { this.pedigreeValidationType = value }

  /** Allow interval processing with an unsupported BAM/CRAM */
  @Argument(fullName = "allow_intervals_with_unindexed_bam", shortName = "", doc = "Allow interval processing with an unsupported BAM/CRAM", required = false, exclusiveOf = "", validation = "")
  var allow_intervals_with_unindexed_bam: Boolean = _

  /** Write a BCF copy of the output VCF */
  @Argument(fullName = "generateShadowBCF", shortName = "generateShadowBCF", doc = "Write a BCF copy of the output VCF", required = false, exclusiveOf = "", validation = "")
  var generateShadowBCF: Boolean = _

  /** Type of IndexCreator to use for VCF/BCF indices */
  @Argument(fullName = "variant_index_type", shortName = "variant_index_type", doc = "Type of IndexCreator to use for VCF/BCF indices", required = false, exclusiveOf = "", validation = "")
  var variant_index_type: org.broadinstitute.gatk.utils.variant.GATKVCFIndexType = _

  /** Parameter to pass to the VCF/BCF IndexCreator */
  @Argument(fullName = "variant_index_parameter", shortName = "variant_index_parameter", doc = "Parameter to pass to the VCF/BCF IndexCreator", required = false, exclusiveOf = "", validation = "")
  var variant_index_parameter: Option[Int] = None

  /** Reference window stop */
  @Argument(fullName = "reference_window_stop", shortName = "ref_win_stop", doc = "Reference window stop", required = false, exclusiveOf = "", validation = "")
  var reference_window_stop: Option[Int] = None

  /**
   * Short name of reference_window_stop
   * @return Short name of reference_window_stop
   */
  def ref_win_stop = this.reference_window_stop

  /**
   * Short name of reference_window_stop
   * @param value Short name of reference_window_stop
   */
  def ref_win_stop_=(value: Option[Int]) { this.reference_window_stop = value }

  /** Set the minimum level of logging */
  @Argument(fullName = "logging_level", shortName = "l", doc = "Set the minimum level of logging", required = false, exclusiveOf = "", validation = "")
  var logging_level: String = _

  /**
   * Short name of logging_level
   * @return Short name of logging_level
   */
  def l = this.logging_level

  /**
   * Short name of logging_level
   * @param value Short name of logging_level
   */
  def l_=(value: String) { this.logging_level = value }

  /** Set the logging location */
  @Output(fullName = "log_to_file", shortName = "log", doc = "Set the logging location", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var log_to_file: File = _

  /**
   * Short name of log_to_file
   * @return Short name of log_to_file
   */
  def log = this.log_to_file

  /**
   * Short name of log_to_file
   * @param value Short name of log_to_file
   */
  def log_=(value: File) { this.log_to_file = value }

  /** Generate the help message */
  @Argument(fullName = "help", shortName = "h", doc = "Generate the help message", required = false, exclusiveOf = "", validation = "")
  var help: Boolean = _

  /**
   * Short name of help
   * @return Short name of help
   */
  def h = this.help

  /**
   * Short name of help
   * @param value Short name of help
   */
  def h_=(value: Boolean) { this.help = value }

  /** Output version information */
  @Argument(fullName = "version", shortName = "version", doc = "Output version information", required = false, exclusiveOf = "", validation = "")
  var version: Boolean = _

  override def freezeFieldValues() {
    super.freezeFieldValues()
    input_fileIndexes ++= input_file.filter(orig => orig != null && orig.getName.endsWith(".bam")).flatMap(orig => Array(new File(orig.getPath + ".bai"), new File(orig.getPath.stripSuffix(".bam") + ".bai")))
    if (num_threads.isDefined) nCoresRequest = num_threads
    if (num_cpu_threads_per_data_thread.isDefined) nCoresRequest = Some(nCoresRequest.getOrElse(1) * num_cpu_threads_per_data_thread.getOrElse(1))
  }

  override def cmdLine = super.cmdLine +
    required("-T", analysis_type, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-I", input_file, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    conditional(showFullBamList, "--showFullBamList", escape = true, format = "%s") +
    optional("-rbs", read_buffer_size, spaceSeparated = true, escape = true, format = "%s") +
    optional("-et", phone_home, spaceSeparated = true, escape = true, format = "%s") +
    optional("-K", gatk_key, spaceSeparated = true, escape = true, format = "%s") +
    optional("-tag", tag, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-rf", read_filter, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-drf", disable_read_filter, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-L", intervals, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-L", intervalsString, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-XL", excludeIntervals, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-XL", excludeIntervalsString, spaceSeparated = true, escape = true, format = "%s") +
    optional("-isr", interval_set_rule, spaceSeparated = true, escape = true, format = "%s") +
    optional("-im", interval_merging, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ip", interval_padding, spaceSeparated = true, escape = true, format = "%s") +
    optional("-R", reference_sequence, spaceSeparated = true, escape = true, format = "%s") +
    conditional(nonDeterministicRandomSeed, "-ndrs", escape = true, format = "%s") +
    conditional(disableDithering, "--disableDithering", escape = true, format = "%s") +
    optional("-maxRuntime", maxRuntime, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxRuntimeUnits", maxRuntimeUnits, spaceSeparated = true, escape = true, format = "%s") +
    optional("-dt", downsampling_type, spaceSeparated = true, escape = true, format = "%s") +
    optional("-dfrac", downsample_to_fraction, spaceSeparated = true, escape = true, format = downsample_to_fractionFormat) +
    optional("-dcov", downsample_to_coverage, spaceSeparated = true, escape = true, format = "%s") +
    optional("-baq", baq, spaceSeparated = true, escape = true, format = "%s") +
    optional("-baqGOP", baqGapOpenPenalty, spaceSeparated = true, escape = true, format = baqGapOpenPenaltyFormat) +
    conditional(refactor_NDN_cigar_string, "-fixNDN", escape = true, format = "%s") +
    conditional(fix_misencoded_quality_scores, "-fixMisencodedQuals", escape = true, format = "%s") +
    conditional(allow_potentially_misencoded_quality_scores, "-allowPotentiallyMisencodedQuals", escape = true, format = "%s") +
    conditional(useOriginalQualities, "-OQ", escape = true, format = "%s") +
    optional("-DBQ", defaultBaseQualities, spaceSeparated = true, escape = true, format = "%s") +
    optional("-PF", performanceLog, spaceSeparated = true, escape = true, format = "%s") +
    optional("-BQSR", BQSR, spaceSeparated = true, escape = true, format = "%s") +
    optional("-qq", quantize_quals, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-SQQ", static_quantized_quals, spaceSeparated = true, escape = true, format = "%s") +
    conditional(round_down_quantized, "-RDQ", escape = true, format = "%s") +
    conditional(disable_indel_quals, "-DIQ", escape = true, format = "%s") +
    conditional(emit_original_quals, "-EOQ", escape = true, format = "%s") +
    optional("-preserveQ", preserve_qscores_less_than, spaceSeparated = true, escape = true, format = "%s") +
    optional("-globalQScorePrior", globalQScorePrior, spaceSeparated = true, escape = true, format = globalQScorePriorFormat) +
    optional("-S", validation_strictness, spaceSeparated = true, escape = true, format = "%s") +
    conditional(remove_program_records, "-rpr", escape = true, format = "%s") +
    conditional(keep_program_records, "-kpr", escape = true, format = "%s") +
    optional("-sample_rename_mapping_file", sample_rename_mapping_file, spaceSeparated = true, escape = true, format = "%s") +
    optional("-U", unsafe, spaceSeparated = true, escape = true, format = "%s") +
    conditional(disable_auto_index_creation_and_locking_when_reading_rods, "-disable_auto_index_creation_and_locking_when_reading_rods", escape = true, format = "%s") +
    conditional(no_cmdline_in_header, "-no_cmdline_in_header", escape = true, format = "%s") +
    conditional(sites_only, "-sites_only", escape = true, format = "%s") +
    conditional(never_trim_vcf_format_field, "-writeFullFormat", escape = true, format = "%s") +
    conditional(bcf, "-bcf", escape = true, format = "%s") +
    optional("-compress", bam_compression, spaceSeparated = true, escape = true, format = "%s") +
    conditional(simplifyBAM, "-simplifyBAM", escape = true, format = "%s") +
    conditional(disable_bam_indexing, "--disable_bam_indexing", escape = true, format = "%s") +
    conditional(generate_md5, "--generate_md5", escape = true, format = "%s") +
    optional("-nt", num_threads, spaceSeparated = true, escape = true, format = "%s") +
    optional("-nct", num_cpu_threads_per_data_thread, spaceSeparated = true, escape = true, format = "%s") +
    optional("-nit", num_io_threads, spaceSeparated = true, escape = true, format = "%s") +
    conditional(monitorThreadEfficiency, "-mte", escape = true, format = "%s") +
    optional("-bfh", num_bam_file_handles, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-rgbl", read_group_black_list, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-ped", pedigree, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-pedString", pedigreeString, spaceSeparated = true, escape = true, format = "%s") +
    optional("-pedValidationType", pedigreeValidationType, spaceSeparated = true, escape = true, format = "%s") +
    conditional(allow_intervals_with_unindexed_bam, "--allow_intervals_with_unindexed_bam", escape = true, format = "%s") +
    conditional(generateShadowBCF, "-generateShadowBCF", escape = true, format = "%s") +
    optional("-variant_index_type", variant_index_type, spaceSeparated = true, escape = true, format = "%s") +
    optional("-variant_index_parameter", variant_index_parameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ref_win_stop", reference_window_stop, spaceSeparated = true, escape = true, format = "%s") +
    optional("-l", logging_level, spaceSeparated = true, escape = true, format = "%s") +
    optional("-log", log_to_file, spaceSeparated = true, escape = true, format = "%s") +
    conditional(help, "-h", escape = true, format = "%s") +
    conditional(version, "-version", escape = true, format = "%s")
}
