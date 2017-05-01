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
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, _ }

class IndelRealigner(val parent: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "IndelRealigner"
  scatterClass = classOf[ContigScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = true }

  /** Input VCF file(s) with known indels */
  @Input(fullName = "knownAlleles", shortName = "known", doc = "Input VCF file(s) with known indels", required = false, exclusiveOf = "", validation = "")
  var knownAlleles: Seq[File] = Nil

  /** Intervals file output from RealignerTargetCreator */
  @Input(fullName = "targetIntervals", shortName = "targetIntervals", doc = "Intervals file output from RealignerTargetCreator", required = true, exclusiveOf = "", validation = "")
  var targetIntervals: File = _

  /** LOD threshold above which the cleaner will clean */
  @Argument(fullName = "LODThresholdForCleaning", shortName = "LOD", doc = "LOD threshold above which the cleaner will clean", required = false, exclusiveOf = "", validation = "")
  var LODThresholdForCleaning: Option[Double] = config("LODThresholdForCleaning")

  /** Format string for LODThresholdForCleaning */
  @Argument(fullName = "LODThresholdForCleaningFormat", shortName = "", doc = "Format string for LODThresholdForCleaning", required = false, exclusiveOf = "", validation = "")
  var LODThresholdForCleaningFormat: String = "%s"

  /** Output bam */
  @Output(fullName = "out", shortName = "o", doc = "Output bam", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[MergeSamFiles])
  var out: File = _

  /** Automatically generated md5 for out */
  @Output(fullName = "outMD5", shortName = "", doc = "Automatically generated md5 for out", required = false, exclusiveOf = "", validation = "")
  @Gather(enabled = false)
  private var outMD5: File = _

  /** Determines how to compute the possible alternate consenses */
  @Argument(fullName = "consensusDeterminationModel", shortName = "model", doc = "Determines how to compute the possible alternate consenses", required = false, exclusiveOf = "", validation = "")
  var consensusDeterminationModel: Option[String] = config("consensusDeterminationModel")

  /** Percentage of mismatches at a locus to be considered having high entropy (0.0 < entropy <= 1.0) */
  @Argument(fullName = "entropyThreshold", shortName = "entropy", doc = "Percentage of mismatches at a locus to be considered having high entropy (0.0 < entropy <= 1.0)", required = false, exclusiveOf = "", validation = "")
  var entropyThreshold: Option[Double] = config("entropyThreshold")

  /** Format string for entropyThreshold */
  @Argument(fullName = "entropyThresholdFormat", shortName = "", doc = "Format string for entropyThreshold", required = false, exclusiveOf = "", validation = "")
  var entropyThresholdFormat: String = "%s"

  /** max reads allowed to be kept in memory at a time by the SAMFileWriter */
  @Argument(fullName = "maxReadsInMemory", shortName = "maxInMemory", doc = "max reads allowed to be kept in memory at a time by the SAMFileWriter", required = false, exclusiveOf = "", validation = "")
  var maxReadsInMemory: Option[Int] = config("maxReadsInMemory")

  /** maximum insert size of read pairs that we attempt to realign */
  @Argument(fullName = "maxIsizeForMovement", shortName = "maxIsize", doc = "maximum insert size of read pairs that we attempt to realign", required = false, exclusiveOf = "", validation = "")
  var maxIsizeForMovement: Option[Int] = config("maxIsizeForMovement")

  /** Maximum positional move in basepairs that a read can be adjusted during realignment */
  @Argument(fullName = "maxPositionalMoveAllowed", shortName = "maxPosMove", doc = "Maximum positional move in basepairs that a read can be adjusted during realignment", required = false, exclusiveOf = "", validation = "")
  var maxPositionalMoveAllowed: Option[Int] = config("maxPositionalMoveAllowed")

  /** Max alternate consensuses to try (necessary to improve performance in deep coverage) */
  @Argument(fullName = "maxConsensuses", shortName = "maxConsensuses", doc = "Max alternate consensuses to try (necessary to improve performance in deep coverage)", required = false, exclusiveOf = "", validation = "")
  var maxConsensuses: Option[Int] = config("maxConsensuses")

  /** Max reads used for finding the alternate consensuses (necessary to improve performance in deep coverage) */
  @Argument(fullName = "maxReadsForConsensuses", shortName = "greedy", doc = "Max reads used for finding the alternate consensuses (necessary to improve performance in deep coverage)", required = false, exclusiveOf = "", validation = "")
  var maxReadsForConsensuses: Option[Int] = config("maxReadsForConsensuses")

  /** Max reads allowed at an interval for realignment */
  @Argument(fullName = "maxReadsForRealignment", shortName = "maxReads", doc = "Max reads allowed at an interval for realignment", required = false, exclusiveOf = "", validation = "")
  var maxReadsForRealignment: Option[Int] = config("maxReadsForRealignment")

  /** Don't output the original cigar or alignment start tags for each realigned read in the output bam */
  @Argument(fullName = "noOriginalAlignmentTags", shortName = "noTags", doc = "Don't output the original cigar or alignment start tags for each realigned read in the output bam", required = false, exclusiveOf = "", validation = "")
  var noOriginalAlignmentTags: Boolean = config("noOriginalAlignmentTags", default = false)

  /** Generate one output file for each input (-I) bam file (not compatible with -output) */
  @Argument(fullName = "nWayOut", shortName = "nWayOut", doc = "Generate one output file for each input (-I) bam file (not compatible with -output)", required = false, exclusiveOf = "", validation = "")
  var nWayOut: Option[String] = config("nWayOut")

  /** Generate md5sums for BAMs */
  @Argument(fullName = "generate_nWayOut_md5s", shortName = "", doc = "Generate md5sums for BAMs", required = false, exclusiveOf = "", validation = "")
  var generate_nWayOut_md5s: Boolean = config("generate_nWayOut_md5s", default = false)

  /** Do early check of reads against existing consensuses */
  @Argument(fullName = "check_early", shortName = "check_early", doc = "Do early check of reads against existing consensuses", required = false, exclusiveOf = "", validation = "")
  var check_early: Boolean = config("check_early", default = false)

  /** Don't output the usual PG tag in the realigned bam file header. FOR DEBUGGING PURPOSES ONLY.  This option is required in order to pass integration tests. */
  @Argument(fullName = "noPGTag", shortName = "noPG", doc = "Don't output the usual PG tag in the realigned bam file header. FOR DEBUGGING PURPOSES ONLY.  This option is required in order to pass integration tests.", required = false, exclusiveOf = "", validation = "")
  var noPGTag: Boolean = config("noPGTag", default = false)

  /** Keep older PG tags left in the bam header by previous runs of this tool (by default, all these historical tags will be replaced by the latest tag generated in the current run). */
  @Argument(fullName = "keepPGTags", shortName = "keepPG", doc = "Keep older PG tags left in the bam header by previous runs of this tool (by default, all these historical tags will be replaced by the latest tag generated in the current run).", required = false, exclusiveOf = "", validation = "")
  var keepPGTags: Boolean = config("keepPGTags", default = false)

  /** Output file (text) for the indels found; FOR DEBUGGING PURPOSES ONLY */
  @Output(fullName = "indelsFileForDebugging", shortName = "indels", doc = "Output file (text) for the indels found; FOR DEBUGGING PURPOSES ONLY", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var indelsFileForDebugging: File = _

  /** print out statistics (what does or doesn't get cleaned); FOR DEBUGGING PURPOSES ONLY */
  @Output(fullName = "statisticsFileForDebugging", shortName = "stats", doc = "print out statistics (what does or doesn't get cleaned); FOR DEBUGGING PURPOSES ONLY", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var statisticsFileForDebugging: File = _

  /** print out whether mismatching columns do or don't get cleaned out; FOR DEBUGGING PURPOSES ONLY */
  @Output(fullName = "SNPsFileForDebugging", shortName = "snps", doc = "print out whether mismatching columns do or don't get cleaned out; FOR DEBUGGING PURPOSES ONLY", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var SNPsFileForDebugging: File = _

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = config("filter_reads_with_N_cigar", default = false)

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = config("filter_mismatching_base_and_quals", default = false)

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = config("filter_bases_not_stored", default = false)

  @Output
  @Gather(enabled = false)
  private var outputIndex: File = _

  override def beforeGraph() {
    super.beforeGraph()
    deps ++= knownAlleles.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (!disable_bam_indexing)
        outputIndex = new File(out.getPath.stripSuffix(".bam") + ".bai")
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
    ir
  }
}
