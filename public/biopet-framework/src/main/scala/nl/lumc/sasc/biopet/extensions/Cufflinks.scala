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
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * Wrapper for the cufflinks command line tool.
 * Written based on cufflinks version v2.2.1 (md5: 07c831c4f8b4e161882731ea5694ff80)
 */
class Cufflinks(val root: Configurable) extends BiopetCommandLineFunction {

  /** default executable */
  executable = config("exe", default = "cufflinks")

  /** input file */
  @Input(doc = "Input file (SAM or BAM)", required = true)
  var input: File = null

  /** output files, computed automatically from output directory */

  @Output(doc = "Output GTF file")
  lazy val outputGtf: File = {
    require(input != null && output_dir != null,
      "Can not set Cufflinks GTF output while input file and/or output directory is not defined")
    // cufflinks always outputs a transcripts.gtf file in the output directory
    new File(output_dir, "transcripts.gtf")
  }

  @Output(doc = "Output isoform FPKM file")
  lazy val outputIsoformsFpkm: File = {
    require(input != null && output_dir != null,
      "Can not set Cufflinks isoforms.fpkm_tracking output while input file and/or output directory is not defined")
    new File(output_dir, "isoforms.fpkm_tracking")
  }

  @Output(doc = "Output GTF file")
  lazy val outputGenesFpkm: File = {
    require(input != null && output_dir != null,
      "Can not set Cufflinks genes.fpkm_tracking output while input file and/or output directory is not defined")
    // cufflinks always outputs a genes.fpkm_tracking file in the output directory
    new File(output_dir, "genes.fpkm_tracking")
  }

  /** write all output files to this directory [./] */
  var output_dir: File = config("output_dir", default = new File("."))

  /** number of threads used during analysis [1] */
  var num_threads: Option[Int] = config("num_threads")

  /** value of random number generator seed [0] */
  var seed: Option[Int] = config("seed")

  /** quantitate against reference transcript annotations */
  var GTF: Option[File] = config("GTF")

  /** use reference transcript annotation to guide assembly */
  var GTF_guide: Option[File] = config("GTF_guide")

  /** ignore all alignment within transcripts in this file */
  var mask_file: Option[File] = config("mask_file")

  /** use bias correction - reference fasta required [NULL] */
  var frag_bias_correct: Option[String] = config("frag_bias_correct")

  /** use 'rescue method' for multi-reads (more accurate) [FALSE] */
  var multi_read_correct: Boolean = config("multi_read_correct", default = false)

  /** library prep used for input reads [below] */
  var library_type: Option[String] = config("library_type")

  /** Method used to normalize library sizes [below] */
  var library_norm_method: Option[String] = config("library_norm_method")

  /** average fragment length (unpaired reads only) [200] */
  var frag_len_mean: Option[Int] = config("frag_len_mean")

  /** fragment length std deviation (unpaired reads only) [80] */
  var frag_len_std_dev: Option[Int] = config("frag_len_std_dev")

  /** maximum iterations allowed for MLE calculation [5000] */
  var max_mle_iterations: Option[Int] = config("max_mle_iterations")

  /** count hits compatible with reference RNAs only [FALSE] */
  var compatible_hits_norm: Boolean = config("compatible_hits_norm", default = false)

  /** count all hits for normalization [TRUE] */
  var total_hits_norm: Boolean = config("total_hits_norm", default = true)

  /** Number of fragment generation samples [100] */
  var num_frag_count_draws: Option[Int] = config("num_frag_count_draws")

  /** Number of fragment assignment samples per generation [50] */
  var num_frag_assign_draws: Option[Int] = config("num_frag_assign_draws")

  /** Maximum number of alignments allowed per fragment [unlim] */
  var max_frag_multihits: Option[Int] = config("max_frag_multihits")

  /** No effective length correction [FALSE] */
  var no_effective_length_correction: Boolean = config("no_effective_length_correction", default = false)

  /** No length correction [FALSE] */
  var no_length_correction: Boolean = config("no_length_correction", default = false)

  /** assembled transcripts have this ID prefix [CUFF] */
  var label: Option[String] = config("label")

  /** suppress transcripts below this abundance level [0.10] */
  var min_isoform_fraction: Option[Float] = config("min_isoform_fraction")

  /** suppress intra-intronic transcripts below this level [0.15] */
  var pre_mrna_fraction: Option[Float] = config("pre_mrna_fraction")

  /** ignore alignments with gaps longer than this [300000] */
  var max_intron_length: Option[Int] = config("max_intron_length")

  /** alpha for junction binomial test filter [0.001] */
  var junc_alpha: Option[Float] = config("junc_alpha")

  /** percent read overhang taken as 'suspiciously small' [0.09] */
  var small_anchor_fraction: Option[Float] = config("small_anchor_fraction")

  /** minimum number of fragments needed for new transfrags [10] */
  var min_frags_per_transfrag: Option[Int] = config("min_frags_per_transfrag")

  /** number of terminal exon bp to tolerate in introns [8] */
  var overhang_tolerance: Option[Int] = config("overhang_tolerance")

  /** maximum genomic length allowed for a given bundle [3500000] */
  var max_bundle_length: Option[Int] = config("max_bundle_length")

  /** maximum fragments allowed in a bundle before skipping [500000] */
  var max_bundle_frags: Option[Int] = config("max_bundle_frags")

  /** minimum intron size allowed in genome [50] */
  var min_intron_length: Option[Int] = config("min_intron_length")

  /** minimum avg coverage required to attempt 3' trimming [10] */
  var trim_3_avgcov_thresh: Option[Int] = config("trim_3_avgcov_thresh")

  /** fraction of avg coverage below which to trim 3' end [0.1] */
  var trim_3_dropoff_frac: Option[Float] = config("trim_3_dropoff_frac")

  /** maximum fraction of allowed multireads per transcript [0.75] */
  var max_multiread_fraction: Option[Float] = config("max_multiread_fraction")

  /** maximum gap size to fill between transfrags (in bp) [50] */
  var overlap_radius: Option[Int] = config("overlap_radius")

  /** disable tiling by faux reads [FALSE] */
  var no_faux_reads: Boolean = config("no_faux_reads", default = false)

  /** overhang allowed on 3' end when merging with reference [600] */
  var flag_3_overhang_tolerance: Option[Int] = config("flag_3_overhang_tolerance")

  /** overhang allowed inside reference intron when merging [30] */
  var intron_overhang_tolerance: Option[Int] = config("intron_overhang_tolerance")

  /** log-friendly verbose processing (no progress bar) [FALSE] */
  var verbose: Boolean = config("verbose", default = false)

  /** log-friendly quiet processing (no progress bar) [FALSE] */
  var quiet: Boolean = config("quiet", default = false)

  /** do not contact server to check for update availability [FALSE] */
  var no_update_check: Boolean = config("no_update_check", default = false)

  override val versionRegex = """cufflinks v(.*)""".r
  override def versionCommand = executable
  override val versionExitcode = List(0, 1)

  def cmdLine =
    required(executable) +
      required("--output-dir", output_dir) +
      optional("--num-threads", num_threads) +
      optional("--seed", seed) +
      optional("--GTF", GTF) +
      optional("--GTF-guide", GTF_guide) +
      optional("--mask-file", mask_file) +
      optional("--frag-bias-correct", frag_bias_correct) +
      conditional(multi_read_correct, "--multi-read-correct") +
      optional("--library-type", library_type) +
      optional("--library-norm-method", library_norm_method) +
      optional("--frag-len-mean", frag_len_mean) +
      optional("--frag-len-std-dev", frag_len_std_dev) +
      optional("--max-mle-iterations", max_mle_iterations) +
      conditional(compatible_hits_norm, "--compatible-hits-norm") +
      conditional(total_hits_norm, "--total-hits-norm") +
      optional("--num-frag-count-draws", num_frag_count_draws) +
      optional("--num-frag-assign-draws", num_frag_assign_draws) +
      optional("--max-frag-multihits", max_frag_multihits) +
      conditional(no_effective_length_correction, "--no-effective-length-correction") +
      conditional(no_length_correction, "--no-length-correction") +
      optional("--label", label) +
      optional("--min-isoform-fraction", min_isoform_fraction) +
      optional("--pre-mrna-fraction", pre_mrna_fraction) +
      optional("--max-intron-length", max_intron_length) +
      optional("--junc-alpha", junc_alpha) +
      optional("--small-anchor-fraction", small_anchor_fraction) +
      optional("--min-frags-per-transfrag", min_frags_per_transfrag) +
      optional("--overhang-tolerance", overhang_tolerance) +
      optional("--max-bundle-length", max_bundle_length) +
      optional("--max-bundle-frags", max_bundle_frags) +
      optional("--min-intron-length", min_intron_length) +
      optional("--trim-3-avgcov-thresh", trim_3_avgcov_thresh) +
      optional("--trim-3-dropoff-frac", trim_3_dropoff_frac) +
      optional("--max-multiread-fraction", max_multiread_fraction) +
      optional("--overlap-radius", overlap_radius) +
      conditional(no_faux_reads, "--no-faux-reads") +
      optional("--flag-3-overhang-tolerance", flag_3_overhang_tolerance) +
      optional("--intron-overhang-tolerance", intron_overhang_tolerance) +
      conditional(verbose, "--verbose") +
      conditional(quiet, "--quiet") +
      conditional(no_update_check, "--no-update-check") +
      required(input)
}
