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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Extension for Tophat
 */
class Tophat(val root: Configurable) extends BiopetCommandLineFunction {

  executable = config("exe", default = "tophat", freeVar = false)

  override val versionRegex = """TopHat v(.*)""".r
  override val versionExitcode = List(0, 1)
  override def versionCommand = executable + " --version"

  override val defaultCoreMemory = 4.0
  override val defaultThreads = 8

  @Input(doc = "FastQ file(s) R1", shortName = "R1")
  var R1: List[File] = List.empty[File]

  @Input(doc = "FastQ file(s) R2", shortName = "R2", required = false)
  var R2: List[File] = List.empty[File]

  private def checkInputsOk(): Unit =
    require(R1.nonEmpty && output_dir != null, "Read 1 input(s) are defined and output directory is defined")

  /** output files, computed automatically from output directory */

  @Output(doc = "Output SAM/BAM file")
  lazy val outputAcceptedHits: File = {
    checkInputsOk()
    new File(output_dir, if (no_convert_bam) "accepted_hits.sam" else "accepted_hits.bam")
  }

  @Output(doc = "Unmapped SAM/BAM file")
  lazy val outputUnmapped: File = {
    checkInputsOk()
    new File(output_dir, if (no_convert_bam) "unmapped.sam" else "unmapped.bam")
  }

  @Output(doc = "Deletions BED file")
  lazy val outputDeletions: File = {
    checkInputsOk()
    new File(output_dir, "deletions.bed")
  }

  @Output(doc = "Insertions BED file")
  lazy val outputInsertions: File = {
    checkInputsOk()
    new File(output_dir, "insertions.bed")
  }

  @Output(doc = "Junctions BED file")
  lazy val outputJunctions: File = {
    checkInputsOk()
    new File(output_dir, "junctions.bed")
  }

  @Output(doc = "Alignment summary file")
  lazy val outputAlignSummary: File = {
    checkInputsOk()
    new File(output_dir, "align_summary.txt")
  }

  @Argument(doc = "Bowtie index", shortName = "bti", required = true)
  var bowtie_index: String = config("bowtie_index")

  /** write all output files to this directory [./] */
  var output_dir: File = config("output_dir", default = new File("tophat_out"))

  var bowtie1: Boolean = config("bowtie1", default = false)

  var read_mismatches: Option[Int] = config("read_mismatches")

  var read_gap_length: Option[Int] = config("read_gap_length")

  var read_edit_dist: Option[Int] = config("read_edit_dist")

  var read_realign_edit_dist: Option[Int] = config("read_realign_edit_dist")

  var min_anchor: Option[Int] = config("min_anchor")

  var splice_mismatches: Option[String] = config("splice_mismatches")

  var min_intron_length: Option[Int] = config("min_intron_length")

  var max_intron_length: Option[Int] = config("max_intron_length")

  var max_multihits: Option[Int] = config("max_multihits")

  var suppress_hits: Boolean = config("suppress_hits", default = false)

  var transcriptome_max_hits: Option[Int] = config("transcriptome_max_hits")

  var prefilter_multihits: Boolean = config("prefilter_multihits", default = false)

  var max_insertion_length: Option[Int] = config("max_insertion_length")

  var max_deletion_length: Option[Int] = config("max_deletion_length")

  var solexa_quals: Boolean = config("solexa_quals", default = false)

  var solexa1_3_quals: Boolean = config("solexa1.3_quals", default = false)

  var phred64_quals: Boolean = config("phred64_quals", default = false)

  var quals: Boolean = config("quals", default = false)

  var integer_quals: Boolean = config("integer_quals", default = false)

  var color: Boolean = config("color", default = false)

  var color_out: Boolean = config("color_out", default = false)

  var library_type: Option[String] = config("library_type")

  var resume: Option[String] = config("resume")

  var GTF: Option[String] = config("GTF")

  var transcriptome_index: Option[String] = config("transcriptome_index")

  var transcriptome_only: Boolean = config("transcriptome_only", default = false)

  var raw_juncs: Option[String] = config("raw_juncs")

  var insertions: Option[String] = config("insertions")

  var deletions: Option[String] = config("deletions")

  var mate_inner_dist: Option[Int] = config("mate_inner_dist")

  var mate_std_dev: Option[Int] = config("mate_std_dev")

  var no_novel_juncs: Boolean = config("no_novel_juncs", default = false)

  var no_novel_indels: Boolean = config("no_novel_indels", default = false)

  var no_gtf_juncs: Boolean = config("no_gtf_juncs", default = false)

  var no_coverage_search: Boolean = config("no_coverage_search", default = false)

  var coverage_search: Boolean = config("coverage_search", default = false)

  var microexon_search: Boolean = config("microexon_search", default = false)

  var keep_tmp: Boolean = config("keep_tmp", default = false)

  var tmp_dir: Option[String] = config("tmp_dir")

  var zpacker: Option[String] = config("zpacker")

  var unmapped_fifo: Boolean = config("unmapped_fifo", default = false)

  var report_secondary_alignments: Boolean = config("report_secondary_alignments", default = false)

  var no_discordant: Boolean = config("no_discordant", default = false)

  var no_mixed: Boolean = config("no_mixed", default = false)

  var segment_mismatches: Option[Int] = config("segment_mismatches")

  var segment_length: Option[Int] = config("segment_length")

  var bowtie_n: Boolean = config("bowtie_n", default = false)

  var min_coverage_intron: Option[Int] = config("min_coverage_intron")

  var max_coverage_intron: Option[Int] = config("max_coverage_intron")

  var min_segment_intron: Option[Int] = config("min_segment_intron")

  var max_segment_intron: Option[Int] = config("max_segment_intron")

  var no_sort_bam: Boolean = config("no_sort_bam", default = false)

  var no_convert_bam: Boolean = config("no_convert_bam", default = false)

  var keep_fasta_order: Boolean = config("keep_fasta_order", default = false)

  var allow_partial_mapping: Boolean = config("allow_partial_mapping", default = false)

  var b2_very_fast: Boolean = config("b2_very_fast", default = false)

  var b2_fast: Boolean = config("b2_fast", default = false)

  var b2_sensitive: Boolean = config("b2_sensitive", default = false)

  var b2_very_sensitive: Boolean = config("b2_very_sensitive", default = false)

  var b2_N: Option[Int] = config("b2_N")

  var b2_L: Option[Int] = config("b2_L")

  var b2_i: Option[String] = config("b2_i")

  var b2_n_ceil: Option[String] = config("b2_n_ceil")

  var b2_gbar: Option[Int] = config("b2_gbar")

  var b2_mp: Option[String] = config("b2_mp")

  var b2_np: Option[Int] = config("b2_np")

  var b2_rdg: Option[String] = config("b2_rdg")

  var b2_rfg: Option[String] = config("b2_rfg")

  var b2_score_min: Option[String] = config("b2_score_min")

  var b2_D: Option[Int] = config("b2_D")

  var b2_R: Option[Int] = config("b2_R")

  var fusion_search: Boolean = config("fusion_search", default = false)

  var fusion_anchor_length: Option[Int] = config("fusion_anchor_length")

  var fusion_min_dist: Option[Int] = config("fusion_min_dist")

  var fusion_read_mismatches: Option[Int] = config("fusion_read_mismatches")

  var fusion_multireads: Option[Int] = config("fusion_multireads")

  var fusion_multipairs: Option[Int] = config("fusion_multipairs")

  var fusion_ignore_chromosomes: Option[String] = config("fusion_ignore_chromosomes")

  var fusion_do_not_resolve_conflicts: Boolean = config("fusion_do_not_resolve_conflicts", default = false)

  var rg_id: Option[String] = config("rg_id")

  var rg_sample: Option[String] = config("rg_sample")

  var rg_library: Option[String] = config("rg_library")

  var rg_description: Option[String] = config("rg_description")

  var rg_platform_unit: Option[String] = config("rg_platform_unit")

  var rg_center: Option[String] = config("rg_center")

  var rg_date: Option[String] = config("rg_date")

  var rg_platform: Option[String] = config("rg_platform")

  def cmdLine: String = required(executable) +
    optional("-o", output_dir) +
    conditional(bowtie1, "--bowtie1") +
    optional("--read-mismatches", read_mismatches) +
    optional("--read-gap-length", read_gap_length) +
    optional("--read-edit-dist", read_edit_dist) +
    optional("--read-realign-edit-dist", read_realign_edit_dist) +
    optional("--min-anchor", min_anchor) +
    optional("--splice-mismatches", splice_mismatches) +
    optional("--min-intron-length", min_intron_length) +
    optional("--max-intron-length", max_intron_length) +
    optional("--max-multihits", max_multihits) +
    conditional(suppress_hits, "--suppress-hits") +
    optional("--transcriptome-max-hits", transcriptome_max_hits) +
    conditional(prefilter_multihits, "--prefilter-multihits") +
    optional("--max-insertion-length", max_insertion_length) +
    optional("--max-deletion-length", max_deletion_length) +
    conditional(solexa_quals, "--solexa-quals") +
    conditional(solexa1_3_quals, "--solexa1.3-quals") +
    conditional(phred64_quals, "--phred64-quals") +
    conditional(quals, "--quals") +
    conditional(integer_quals, "--integer-quals") +
    conditional(color, "--color") +
    conditional(color_out, "--color-out") +
    optional("--library-type", library_type) +
    optional("--num-threads", threads) +
    optional("--resume", resume) +
    optional("--GTF", GTF) +
    optional("--transcriptome-index", transcriptome_index) +
    conditional(transcriptome_only, "--transcriptome-only") +
    optional("--raw-juncs", raw_juncs) +
    optional("--insertions", insertions) +
    optional("--deletions", deletions) +
    optional("--mate-inner-dist", mate_inner_dist) +
    optional("--mate-std-dev", mate_std_dev) +
    conditional(no_novel_juncs, "--no-novel-juncs") +
    conditional(no_novel_indels, "--no-novel-indels") +
    conditional(no_gtf_juncs, "--no-gtf-juncs") +
    conditional(no_coverage_search, "--no-coverage-search") +
    conditional(coverage_search, "--coverage-search") +
    conditional(microexon_search, "--microexon-search") +
    conditional(keep_tmp, "--keep-tmp") +
    optional("--tmp-dir", tmp_dir) +
    optional("--zpacker", zpacker) +
    conditional(unmapped_fifo, "--unmapped-fifo") +
    conditional(report_secondary_alignments, "--report-secondary-alignments") +
    conditional(no_discordant, "--no-discordant") +
    conditional(no_mixed, "--no-mixed") +
    optional("--segment-mismatches", segment_mismatches) +
    optional("--segment-length", segment_length) +
    conditional(bowtie_n, "--bowtie-n") +
    optional("--min-coverage-intron", min_coverage_intron) +
    optional("--max-coverage-intron", max_coverage_intron) +
    optional("--min-segment-intron", min_segment_intron) +
    optional("--max-segment-intron", max_segment_intron) +
    conditional(no_sort_bam, "--no-sort-bam") +
    conditional(no_convert_bam, "--no-convert-bam") +
    conditional(keep_fasta_order, "--keep-fasta-order") +
    conditional(allow_partial_mapping, "--allow-partial-mapping") +
    conditional(b2_very_fast, "--b2-very-fast") +
    conditional(b2_fast, "--b2-fast") +
    conditional(b2_sensitive, "--b2-sensitive") +
    conditional(b2_very_sensitive, "--b2-very-sensitive") +
    optional("--b2-N", b2_N) +
    optional("--b2-L", b2_L) +
    optional("--b2-i", b2_i) +
    optional("--b2-n-ceil", b2_n_ceil) +
    optional("--b2-gbar", b2_gbar) +
    optional("--b2-mp", b2_mp) +
    optional("--b2-np", b2_np) +
    optional("--b2-rdg", b2_rdg) +
    optional("--b2-rfg", b2_rfg) +
    optional("--b2-score-min", b2_score_min) +
    optional("--b2-D", b2_D) +
    optional("--b2-R", b2_R) +
    conditional(fusion_search, "--fusion-search") +
    optional("--fusion-anchor-length", fusion_anchor_length) +
    optional("--fusion-min-dist", fusion_min_dist) +
    optional("--fusion-read-mismatches", fusion_read_mismatches) +
    optional("--fusion-multireads", fusion_multireads) +
    optional("--fusion-multipairs", fusion_multipairs) +
    optional("--fusion-ignore-chromosomes", fusion_ignore_chromosomes) +
    conditional(fusion_do_not_resolve_conflicts, "--fusion-do-not-resolve-conflicts") +
    optional("--rg-id", rg_id) +
    optional("--rg-sample", rg_sample) +
    optional("--rg-library", rg_library) +
    optional("--rg-description", rg_description) +
    optional("--rg-platform-unit", rg_platform_unit) +
    optional("--rg-center", rg_center) +
    optional("--rg-date", rg_date) +
    optional("--rg-platform", rg_platform) +
    required(bowtie_index) +
    required(R1.mkString(",")) +
    optional(R2.mkString(","))
}
