/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ BamGatherFunction, GATKScatterFunction, ReadScatterFunction, TaggedFile }
import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, _ }

class IndelRealigner(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  analysisName = "IndelRealigner"
  analysis_type = "IndelRealigner"
  scatterClass = classOf[ReadScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = true }

  /** Input VCF file(s) with known indels */
  @Input(fullName = "knownAlleles", shortName = "known", doc = "Input VCF file(s) with known indels", required = false, exclusiveOf = "", validation = "")
  var knownAlleles: Seq[File] = Nil

  /**
   * Short name of knownAlleles
   * @return Short name of knownAlleles
   */
  def known = this.knownAlleles

  /**
   * Short name of knownAlleles
   * @param value Short name of knownAlleles
   */
  def known_=(value: Seq[File]) { this.knownAlleles = value }

  /** Dependencies on any indexes of knownAlleles */
  @Input(fullName = "knownAllelesIndexes", shortName = "", doc = "Dependencies on any indexes of knownAlleles", required = false, exclusiveOf = "", validation = "")
  private var knownAllelesIndexes: Seq[File] = Nil

  /** Intervals file output from RealignerTargetCreator */
  @Input(fullName = "targetIntervals", shortName = "targetIntervals", doc = "Intervals file output from RealignerTargetCreator", required = true, exclusiveOf = "", validation = "")
  var targetIntervals: File = _

  /** LOD threshold above which the cleaner will clean */
  @Argument(fullName = "LODThresholdForCleaning", shortName = "LOD", doc = "LOD threshold above which the cleaner will clean", required = false, exclusiveOf = "", validation = "")
  var LODThresholdForCleaning: Option[Double] = None

  /**
   * Short name of LODThresholdForCleaning
   * @return Short name of LODThresholdForCleaning
   */
  def LOD = this.LODThresholdForCleaning

  /**
   * Short name of LODThresholdForCleaning
   * @param value Short name of LODThresholdForCleaning
   */
  def LOD_=(value: Option[Double]) { this.LODThresholdForCleaning = value }

  /** Format string for LODThresholdForCleaning */
  @Argument(fullName = "LODThresholdForCleaningFormat", shortName = "", doc = "Format string for LODThresholdForCleaning", required = false, exclusiveOf = "", validation = "")
  var LODThresholdForCleaningFormat: String = "%s"

  /** Output bam */
  @Output(fullName = "out", shortName = "o", doc = "Output bam", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[BamGatherFunction])
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

  /** Automatically generated md5 for out */
  @Output(fullName = "outMD5", shortName = "", doc = "Automatically generated md5 for out", required = false, exclusiveOf = "", validation = "")
  @Gather(enabled = false)
  private var outMD5: File = _

  /** Determines how to compute the possible alternate consenses */
  @Argument(fullName = "consensusDeterminationModel", shortName = "model", doc = "Determines how to compute the possible alternate consenses", required = false, exclusiveOf = "", validation = "")
  var consensusDeterminationModel: String = _

  /**
   * Short name of consensusDeterminationModel
   * @return Short name of consensusDeterminationModel
   */
  def model = this.consensusDeterminationModel

  /**
   * Short name of consensusDeterminationModel
   * @param value Short name of consensusDeterminationModel
   */
  def model_=(value: String) { this.consensusDeterminationModel = value }

  /** Percentage of mismatches at a locus to be considered having high entropy (0.0 < entropy <= 1.0) */
  @Argument(fullName = "entropyThreshold", shortName = "entropy", doc = "Percentage of mismatches at a locus to be considered having high entropy (0.0 < entropy <= 1.0)", required = false, exclusiveOf = "", validation = "")
  var entropyThreshold: Option[Double] = None

  /**
   * Short name of entropyThreshold
   * @return Short name of entropyThreshold
   */
  def entropy = this.entropyThreshold

  /**
   * Short name of entropyThreshold
   * @param value Short name of entropyThreshold
   */
  def entropy_=(value: Option[Double]) { this.entropyThreshold = value }

  /** Format string for entropyThreshold */
  @Argument(fullName = "entropyThresholdFormat", shortName = "", doc = "Format string for entropyThreshold", required = false, exclusiveOf = "", validation = "")
  var entropyThresholdFormat: String = "%s"

  /** max reads allowed to be kept in memory at a time by the SAMFileWriter */
  @Argument(fullName = "maxReadsInMemory", shortName = "maxInMemory", doc = "max reads allowed to be kept in memory at a time by the SAMFileWriter", required = false, exclusiveOf = "", validation = "")
  var maxReadsInMemory: Option[Int] = None

  /**
   * Short name of maxReadsInMemory
   * @return Short name of maxReadsInMemory
   */
  def maxInMemory = this.maxReadsInMemory

  /**
   * Short name of maxReadsInMemory
   * @param value Short name of maxReadsInMemory
   */
  def maxInMemory_=(value: Option[Int]) { this.maxReadsInMemory = value }

  /** maximum insert size of read pairs that we attempt to realign */
  @Argument(fullName = "maxIsizeForMovement", shortName = "maxIsize", doc = "maximum insert size of read pairs that we attempt to realign", required = false, exclusiveOf = "", validation = "")
  var maxIsizeForMovement: Option[Int] = None

  /**
   * Short name of maxIsizeForMovement
   * @return Short name of maxIsizeForMovement
   */
  def maxIsize = this.maxIsizeForMovement

  /**
   * Short name of maxIsizeForMovement
   * @param value Short name of maxIsizeForMovement
   */
  def maxIsize_=(value: Option[Int]) { this.maxIsizeForMovement = value }

  /** Maximum positional move in basepairs that a read can be adjusted during realignment */
  @Argument(fullName = "maxPositionalMoveAllowed", shortName = "maxPosMove", doc = "Maximum positional move in basepairs that a read can be adjusted during realignment", required = false, exclusiveOf = "", validation = "")
  var maxPositionalMoveAllowed: Option[Int] = None

  /**
   * Short name of maxPositionalMoveAllowed
   * @return Short name of maxPositionalMoveAllowed
   */
  def maxPosMove = this.maxPositionalMoveAllowed

  /**
   * Short name of maxPositionalMoveAllowed
   * @param value Short name of maxPositionalMoveAllowed
   */
  def maxPosMove_=(value: Option[Int]) { this.maxPositionalMoveAllowed = value }

  /** Max alternate consensuses to try (necessary to improve performance in deep coverage) */
  @Argument(fullName = "maxConsensuses", shortName = "maxConsensuses", doc = "Max alternate consensuses to try (necessary to improve performance in deep coverage)", required = false, exclusiveOf = "", validation = "")
  var maxConsensuses: Option[Int] = None

  /** Max reads used for finding the alternate consensuses (necessary to improve performance in deep coverage) */
  @Argument(fullName = "maxReadsForConsensuses", shortName = "greedy", doc = "Max reads used for finding the alternate consensuses (necessary to improve performance in deep coverage)", required = false, exclusiveOf = "", validation = "")
  var maxReadsForConsensuses: Option[Int] = None

  /**
   * Short name of maxReadsForConsensuses
   * @return Short name of maxReadsForConsensuses
   */
  def greedy = this.maxReadsForConsensuses

  /**
   * Short name of maxReadsForConsensuses
   * @param value Short name of maxReadsForConsensuses
   */
  def greedy_=(value: Option[Int]) { this.maxReadsForConsensuses = value }

  /** Max reads allowed at an interval for realignment */
  @Argument(fullName = "maxReadsForRealignment", shortName = "maxReads", doc = "Max reads allowed at an interval for realignment", required = false, exclusiveOf = "", validation = "")
  var maxReadsForRealignment: Option[Int] = None

  /**
   * Short name of maxReadsForRealignment
   * @return Short name of maxReadsForRealignment
   */
  def maxReads = this.maxReadsForRealignment

  /**
   * Short name of maxReadsForRealignment
   * @param value Short name of maxReadsForRealignment
   */
  def maxReads_=(value: Option[Int]) { this.maxReadsForRealignment = value }

  /** Don't output the original cigar or alignment start tags for each realigned read in the output bam */
  @Argument(fullName = "noOriginalAlignmentTags", shortName = "noTags", doc = "Don't output the original cigar or alignment start tags for each realigned read in the output bam", required = false, exclusiveOf = "", validation = "")
  var noOriginalAlignmentTags: Boolean = _

  /**
   * Short name of noOriginalAlignmentTags
   * @return Short name of noOriginalAlignmentTags
   */
  def noTags = this.noOriginalAlignmentTags

  /**
   * Short name of noOriginalAlignmentTags
   * @param value Short name of noOriginalAlignmentTags
   */
  def noTags_=(value: Boolean) { this.noOriginalAlignmentTags = value }

  /** Generate one output file for each input (-I) bam file (not compatible with -output) */
  @Argument(fullName = "nWayOut", shortName = "nWayOut", doc = "Generate one output file for each input (-I) bam file (not compatible with -output)", required = false, exclusiveOf = "", validation = "")
  var nWayOut: String = _

  /** Generate md5sums for BAMs */
  @Argument(fullName = "generate_nWayOut_md5s", shortName = "", doc = "Generate md5sums for BAMs", required = false, exclusiveOf = "", validation = "")
  var generate_nWayOut_md5s: Boolean = _

  /** Do early check of reads against existing consensuses */
  @Argument(fullName = "check_early", shortName = "check_early", doc = "Do early check of reads against existing consensuses", required = false, exclusiveOf = "", validation = "")
  var check_early: Boolean = _

  /** Don't output the usual PG tag in the realigned bam file header. FOR DEBUGGING PURPOSES ONLY.  This option is required in order to pass integration tests. */
  @Argument(fullName = "noPGTag", shortName = "noPG", doc = "Don't output the usual PG tag in the realigned bam file header. FOR DEBUGGING PURPOSES ONLY.  This option is required in order to pass integration tests.", required = false, exclusiveOf = "", validation = "")
  var noPGTag: Boolean = _

  /**
   * Short name of noPGTag
   * @return Short name of noPGTag
   */
  def noPG = this.noPGTag

  /**
   * Short name of noPGTag
   * @param value Short name of noPGTag
   */
  def noPG_=(value: Boolean) { this.noPGTag = value }

  /** Keep older PG tags left in the bam header by previous runs of this tool (by default, all these historical tags will be replaced by the latest tag generated in the current run). */
  @Argument(fullName = "keepPGTags", shortName = "keepPG", doc = "Keep older PG tags left in the bam header by previous runs of this tool (by default, all these historical tags will be replaced by the latest tag generated in the current run).", required = false, exclusiveOf = "", validation = "")
  var keepPGTags: Boolean = _

  /**
   * Short name of keepPGTags
   * @return Short name of keepPGTags
   */
  def keepPG = this.keepPGTags

  /**
   * Short name of keepPGTags
   * @param value Short name of keepPGTags
   */
  def keepPG_=(value: Boolean) { this.keepPGTags = value }

  /** Output file (text) for the indels found; FOR DEBUGGING PURPOSES ONLY */
  @Output(fullName = "indelsFileForDebugging", shortName = "indels", doc = "Output file (text) for the indels found; FOR DEBUGGING PURPOSES ONLY", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var indelsFileForDebugging: File = _

  /**
   * Short name of indelsFileForDebugging
   * @return Short name of indelsFileForDebugging
   */
  def indels = this.indelsFileForDebugging

  /**
   * Short name of indelsFileForDebugging
   * @param value Short name of indelsFileForDebugging
   */
  def indels_=(value: File) { this.indelsFileForDebugging = value }

  /** print out statistics (what does or doesn't get cleaned); FOR DEBUGGING PURPOSES ONLY */
  @Output(fullName = "statisticsFileForDebugging", shortName = "stats", doc = "print out statistics (what does or doesn't get cleaned); FOR DEBUGGING PURPOSES ONLY", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var statisticsFileForDebugging: File = _

  /**
   * Short name of statisticsFileForDebugging
   * @return Short name of statisticsFileForDebugging
   */
  def stats = this.statisticsFileForDebugging

  /**
   * Short name of statisticsFileForDebugging
   * @param value Short name of statisticsFileForDebugging
   */
  def stats_=(value: File) { this.statisticsFileForDebugging = value }

  /** print out whether mismatching columns do or don't get cleaned out; FOR DEBUGGING PURPOSES ONLY */
  @Output(fullName = "SNPsFileForDebugging", shortName = "snps", doc = "print out whether mismatching columns do or don't get cleaned out; FOR DEBUGGING PURPOSES ONLY", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var SNPsFileForDebugging: File = _

  /**
   * Short name of SNPsFileForDebugging
   * @return Short name of SNPsFileForDebugging
   */
  def snps = this.SNPsFileForDebugging

  /**
   * Short name of SNPsFileForDebugging
   * @param value Short name of SNPsFileForDebugging
   */
  def snps_=(value: File) { this.SNPsFileForDebugging = value }

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
    knownAllelesIndexes ++= knownAlleles.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (!disable_bam_indexing)
        outIndex = new File(out.getPath.stripSuffix(".bam") + ".bai")
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (generate_md5)
        outMD5 = new File(out.getPath + ".md5")
  }

  override def cmdLine = super.cmdLine +
    repeat("-known", knownAlleles, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    required("-targetIntervals", targetIntervals, spaceSeparated = true, escape = true, format = "%s") +
    optional("-LOD", LODThresholdForCleaning, spaceSeparated = true, escape = true, format = LODThresholdForCleaningFormat) +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    optional("-model", consensusDeterminationModel, spaceSeparated = true, escape = true, format = "%s") +
    optional("-entropy", entropyThreshold, spaceSeparated = true, escape = true, format = entropyThresholdFormat) +
    optional("-maxInMemory", maxReadsInMemory, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxIsize", maxIsizeForMovement, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxPosMove", maxPositionalMoveAllowed, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxConsensuses", maxConsensuses, spaceSeparated = true, escape = true, format = "%s") +
    optional("-greedy", maxReadsForConsensuses, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxReads", maxReadsForRealignment, spaceSeparated = true, escape = true, format = "%s") +
    conditional(noOriginalAlignmentTags, "-noTags", escape = true, format = "%s") +
    optional("-nWayOut", nWayOut, spaceSeparated = true, escape = true, format = "%s") +
    conditional(generate_nWayOut_md5s, "--generate_nWayOut_md5s", escape = true, format = "%s") +
    conditional(check_early, "-check_early", escape = true, format = "%s") +
    conditional(noPGTag, "-noPG", escape = true, format = "%s") +
    conditional(keepPGTags, "-keepPG", escape = true, format = "%s") +
    optional("-indels", indelsFileForDebugging, spaceSeparated = true, escape = true, format = "%s") +
    optional("-stats", statisticsFileForDebugging, spaceSeparated = true, escape = true, format = "%s") +
    optional("-snps", SNPsFileForDebugging, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}

object IndelRealigner {
  def apply(root: Configurable, input: File, targetIntervals: File, outputDir: File): IndelRealigner = {
    val ir = new IndelRealigner(root)
    ir.input_file :+= input
    ir.targetIntervals = targetIntervals
    ir.out = new File(outputDir, input.getName.stripSuffix(".bam") + ".realign.bam")
    ir.deps :+= new File(outputDir, input.getName.stripSuffix(".bam") + ".realign.bai")
    ir
  }
}
