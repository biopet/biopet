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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{Version, Reference, BiopetCommandLineFunction}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * Extension for Tophat
  */
class Tophat(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Reference
    with Version {

  executable = config("exe", default = "tophat", freeVar = false)

  def versionRegex = """TopHat v(.*)""".r
  override def versionExitcode = List(0, 1)
  def versionCommand = executable + " --version"

  override def defaultCoreMemory = 4.0
  override def defaultThreads = 8

  @Input(doc = "FastQ file(s) R1", shortName = "R1")
  var R1: List[File] = List.empty[File]

  @Input(doc = "FastQ file(s) R2", shortName = "R2", required = false)
  var R2: List[File] = List.empty[File]

  private def checkInputsOk(): Unit =
    require(R1.nonEmpty && outputDir != null,
            "Read 1 input(s) are defined and output directory is defined")

  /** output files, computed automatically from output directory */
  @Output(doc = "Output SAM/BAM file")
  lazy val outputAcceptedHits: File = {
    checkInputsOk()
    new File(outputDir, if (noConvertBam) "accepted_hits.sam" else "accepted_hits.bam")
  }

  @Output(doc = "Unmapped SAM/BAM file")
  lazy val outputUnmapped: File = {
    checkInputsOk()
    new File(outputDir, if (noConvertBam) "unmapped.sam" else "unmapped.bam")
  }

  @Output(doc = "Deletions BED file")
  lazy val outputDeletions: File = {
    checkInputsOk()
    new File(outputDir, "deletions.bed")
  }

  @Output(doc = "Insertions BED file")
  lazy val outputInsertions: File = {
    checkInputsOk()
    new File(outputDir, "insertions.bed")
  }

  @Output(doc = "Junctions BED file")
  lazy val outputJunctions: File = {
    checkInputsOk()
    new File(outputDir, "junctions.bed")
  }

  @Output(doc = "Alignment summary file")
  lazy val outputAlignSummary: File = {
    checkInputsOk()
    new File(outputDir, "align_summary.txt")
  }

  @Argument(doc = "Bowtie index", shortName = "bti", required = true)
  var bowtieIndex: String = config("bowtie_index")

  /** write all output files to this directory [./] */
  var outputDir: File = config("output_dir", default = new File("tophat_out"))

  var bowtie1: Boolean = config("bowtie1", default = false)

  var readMismatches: Option[Int] = config("read_mismatches")

  var readGapLength: Option[Int] = config("read_gap_length")

  var readEditDist: Option[Int] = config("read_edit_dist")

  var readRealignEditDist: Option[Int] = config("read_realign_edit_dist")

  var minAnchor: Option[Int] = config("min_anchor")

  var spliceMismatches: Option[String] = config("splice_mismatches")

  var minIntronLength: Option[Int] = config("min_intron_length")

  var maxIntronLength: Option[Int] = config("max_intron_length")

  var maxMultihits: Option[Int] = config("max_multihits")

  var suppressHits: Boolean = config("suppress_hits", default = false)

  var transcriptomeMaxHits: Option[Int] = config("transcriptome_max_hits")

  var preFilterMultihits: Boolean = config("prefilter_multihits", default = false)

  var maxInsertionLength: Option[Int] = config("max_insertion_length")

  var maxDeletionLength: Option[Int] = config("max_deletion_length")

  var solexaQuals: Boolean = config("solexa_quals", default = false)

  var solexa13Quals: Boolean = config("solexa1.3_quals", default = false)

  var phred64Quals: Boolean = config("phred64_quals", default = false)

  var quals: Boolean = config("quals", default = false)

  var integerQuals: Boolean = config("integer_quals", default = false)

  var color: Boolean = config("color", default = false)

  var colorOut: Boolean = config("color_out", default = false)

  var libraryType: Option[String] = config("library_type")

  var resume: Option[String] = config("resume")

  var GTF: Option[String] = config("GTF")

  var transcriptomeIndex: Option[String] = config("transcriptome_index")

  var transcriptomeOnly: Boolean = config("transcriptome_only", default = false)

  var rawJuncs: Option[String] = config("raw_juncs")

  var insertions: Option[String] = config("insertions")

  var deletions: Option[String] = config("deletions")

  var mateInnerDist: Option[Int] = config("mate_inner_dist")

  var mateStdDev: Option[Int] = config("mate_std_dev")

  var noNovelJuncs: Boolean = config("no_novel_juncs", default = false)

  var noNovelIndels: Boolean = config("no_novel_indels", default = false)

  var noGtfJuncs: Boolean = config("no_gtf_juncs", default = false)

  var noCoverageSearch: Boolean = config("no_coverage_search", default = false)

  var coverageSearch: Boolean = config("coverage_search", default = false)

  var microexonSearch: Boolean = config("microexon_search", default = false)

  var keepTmp: Boolean = config("keep_tmp", default = false)

  var tmpDir: Option[String] = config("tmp_dir")

  var zpacker: Option[String] = config("zpacker")

  var unmappedFifo: Boolean = config("unmapped_fifo", default = false)

  var reportSecondaryAlignments: Boolean = config("report_secondary_alignments", default = false)

  var noDiscordant: Boolean = config("no_discordant", default = false)

  var noMixed: Boolean = config("no_mixed", default = false)

  var segmentMismatches: Option[Int] = config("segment_mismatches")

  var segmentLength: Option[Int] = config("segment_length")

  var bowtieN: Boolean = config("bowtie_n", default = false)

  var minCoverageIntron: Option[Int] = config("min_coverage_intron")

  var maxCoverageIntron: Option[Int] = config("max_coverage_intron")

  var minSegmentIntron: Option[Int] = config("min_segment_intron")

  var maxSegmentIntron: Option[Int] = config("max_segment_intron")

  var noSortBam: Boolean = config("no_sort_bam", default = false)

  var noConvertBam: Boolean = config("no_convert_bam", default = false)

  var keepFastaOrder: Boolean = config("keep_fasta_order", default = false)

  var allowPartialMapping: Boolean = config("allow_partial_mapping", default = false)

  var b2VeryFast: Boolean = config("b2_very_fast", default = false)

  var b2Fast: Boolean = config("b2_fast", default = false)

  var b2Sensitive: Boolean = config("b2_sensitive", default = false)

  var b2VerySensitive: Boolean = config("b2_very_sensitive", default = false)

  var b2N: Option[Int] = config("b2_N")

  var b2L: Option[Int] = config("b2_L")

  var b2I: Option[String] = config("b2_i")

  var b2NCeil: Option[String] = config("b2_n_ceil")

  var b2Gbar: Option[Int] = config("b2_gbar")

  var b2Mp: Option[String] = config("b2_mp")

  var b2Np: Option[Int] = config("b2_np")

  var b2Rdg: Option[String] = config("b2_rdg")

  var b2Rfg: Option[String] = config("b2_rfg")

  var b2ScoreMin: Option[String] = config("b2_score_min")

  var b2D: Option[Int] = config("b2_D")

  var b2R: Option[Int] = config("b2_R")

  var fusionSearch: Boolean = config("fusion_search", default = false)

  var fusionAnchorLength: Option[Int] = config("fusion_anchor_length")

  var fusionMinDist: Option[Int] = config("fusion_min_dist")

  var fusionReadMismatches: Option[Int] = config("fusion_read_mismatches")

  var fusionMultireads: Option[Int] = config("fusion_multireads")

  var fusionMultipairs: Option[Int] = config("fusion_multipairs")

  var fusionIgnoreChromosomes: Option[String] = config("fusion_ignore_chromosomes")

  var fusionDoNotResolveConflicts: Boolean =
    config("fusion_do_not_resolve_conflicts", default = false)

  var rgId: Option[String] = config("rg_id")

  var rgSample: Option[String] = config("rg_sample")

  var rgLibrary: Option[String] = config("rg_library")

  var rgDescription: Option[String] = config("rg_description")

  var rgPlatformUnit: Option[String] = config("rg_platform_unit")

  var rgCenter: Option[String] = config("rg_center")

  var rgDate: Option[String] = config("rg_date")

  var rgPlatform: Option[String] = config("rg_platform")

  override def beforeGraph: Unit = {
    super.beforeGraph
    if (bowtie1 && !new File(bowtieIndex).getParentFile
          .list()
          .toList
          .filter(_.startsWith(new File(bowtieIndex).getName))
          .exists(_.endsWith(".ebwt")))
      throw new IllegalArgumentException("No bowtie1 index found for tophat")
    else if (!new File(bowtieIndex).getParentFile
               .list()
               .toList
               .filter(_.startsWith(new File(bowtieIndex).getName))
               .exists(_.endsWith(".bt2")))
      throw new IllegalArgumentException("No bowtie2 index found for tophat")
    if (R2.nonEmpty && mateInnerDist.isEmpty) {
      logger.warn(
        "The parameter 'mate_inner_dist' is not set in the configuration, TopHat will use it's default value - 50bp, please check if this value for inner mate distance is correct for the current data.")
    }
  }

  def cmdLine: String =
    required(executable) +
      optional("-o", outputDir) +
      conditional(bowtie1, "--bowtie1") +
      optional("--read-mismatches", readMismatches) +
      optional("--read-gap-length", readGapLength) +
      optional("--read-edit-dist", readEditDist) +
      optional("--read-realign-edit-dist", readRealignEditDist) +
      optional("--min-anchor", minAnchor) +
      optional("--splice-mismatches", spliceMismatches) +
      optional("--min-intron-length", minIntronLength) +
      optional("--max-intron-length", maxIntronLength) +
      optional("--max-multihits", maxMultihits) +
      conditional(suppressHits, "--suppress-hits") +
      optional("--transcriptome-max-hits", transcriptomeMaxHits) +
      conditional(preFilterMultihits, "--prefilter-multihits") +
      optional("--max-insertion-length", maxInsertionLength) +
      optional("--max-deletion-length", maxDeletionLength) +
      conditional(solexaQuals, "--solexa-quals") +
      conditional(solexa13Quals, "--solexa1.3-quals") +
      conditional(phred64Quals, "--phred64-quals") +
      conditional(quals, "--quals") +
      conditional(integerQuals, "--integer-quals") +
      conditional(color, "--color") +
      conditional(colorOut, "--color-out") +
      optional("--library-type", libraryType) +
      optional("--num-threads", threads) +
      optional("--resume", resume) +
      optional("--GTF", GTF) +
      optional("--transcriptome-index", transcriptomeIndex) +
      conditional(transcriptomeOnly, "--transcriptome-only") +
      optional("--raw-juncs", rawJuncs) +
      optional("--insertions", insertions) +
      optional("--deletions", deletions) +
      optional("--mate-inner-dist", mateInnerDist) +
      optional("--mate-std-dev", mateStdDev) +
      conditional(noNovelJuncs, "--no-novel-juncs") +
      conditional(noNovelIndels, "--no-novel-indels") +
      conditional(noGtfJuncs, "--no-gtf-juncs") +
      conditional(noCoverageSearch, "--no-coverage-search") +
      conditional(coverageSearch, "--coverage-search") +
      conditional(microexonSearch, "--microexon-search") +
      conditional(keepTmp, "--keep-tmp") +
      optional("--tmp-dir", tmpDir) +
      optional("--zpacker", zpacker) +
      conditional(unmappedFifo, "--unmapped-fifo") +
      conditional(reportSecondaryAlignments, "--report-secondary-alignments") +
      conditional(noDiscordant, "--no-discordant") +
      conditional(noMixed, "--no-mixed") +
      optional("--segment-mismatches", segmentMismatches) +
      optional("--segment-length", segmentLength) +
      conditional(bowtieN, "--bowtie-n") +
      optional("--min-coverage-intron", minCoverageIntron) +
      optional("--max-coverage-intron", maxCoverageIntron) +
      optional("--min-segment-intron", minSegmentIntron) +
      optional("--max-segment-intron", maxSegmentIntron) +
      conditional(noSortBam, "--no-sort-bam") +
      conditional(noConvertBam, "--no-convert-bam") +
      conditional(keepFastaOrder, "--keep-fasta-order") +
      conditional(allowPartialMapping, "--allow-partial-mapping") +
      conditional(b2VeryFast, "--b2-very-fast") +
      conditional(b2Fast, "--b2-fast") +
      conditional(b2Sensitive, "--b2-sensitive") +
      conditional(b2VerySensitive, "--b2-very-sensitive") +
      optional("--b2-N", b2N) +
      optional("--b2-L", b2L) +
      optional("--b2-i", b2I) +
      optional("--b2-n-ceil", b2NCeil) +
      optional("--b2-gbar", b2Gbar) +
      optional("--b2-mp", b2Mp) +
      optional("--b2-np", b2Np) +
      optional("--b2-rdg", b2Rdg) +
      optional("--b2-rfg", b2Rfg) +
      optional("--b2-score-min", b2ScoreMin) +
      optional("--b2-D", b2D) +
      optional("--b2-R", b2R) +
      conditional(fusionSearch, "--fusion-search") +
      optional("--fusion-anchor-length", fusionAnchorLength) +
      optional("--fusion-min-dist", fusionMinDist) +
      optional("--fusion-read-mismatches", fusionReadMismatches) +
      optional("--fusion-multireads", fusionMultireads) +
      optional("--fusion-multipairs", fusionMultipairs) +
      optional("--fusion-ignore-chromosomes", fusionIgnoreChromosomes) +
      conditional(fusionDoNotResolveConflicts, "--fusion-do-not-resolve-conflicts") +
      optional("--rg-id", rgId) +
      optional("--rg-sample", rgSample) +
      optional("--rg-library", rgLibrary) +
      optional("--rg-description", rgDescription) +
      optional("--rg-platform-unit", rgPlatformUnit) +
      optional("--rg-center", rgCenter) +
      optional("--rg-date", rgDate) +
      optional("--rg-platform", rgPlatform) +
      required(bowtieIndex) +
      required(R1.mkString(",")) +
      optional(R2.mkString(","))
}
