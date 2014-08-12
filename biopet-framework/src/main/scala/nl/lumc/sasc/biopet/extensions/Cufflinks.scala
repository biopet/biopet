/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.extensions

import java.io.File
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * Wrapper for the cufflinks command line tool.
 * Written based on cufflinks version v2.2.1.
 */
class Cufflinks(val root: Configurable) extends BiopetCommandLineFunction {
  
  /** default executable */
  executable = config("exe", default = "cufflinks")

  /** input file */
  @Input(doc = "Input file (SAM or BAM)", required = true)
  var input: File = _

  /** output files, computed automatically from output directory */

  @Output(doc = "Output GTF file")
  lazy val output_gtf: File = {
    if (input == null || output_dir == null)
      throw new RuntimeException("Unexpected error when trying to set cufflinks GTF output")
    // cufflinks always outputs a transcripts.gtf file in the output directory
    new File(output_dir + File.pathSeparator + "transcripts.gtf")
  }

  @Output(doc = "Output isoform FPKM file")
  lazy val output_isoforms_fpkm: File = {
    if (input == null || output_dir == null)
      throw new RuntimeException("Unexpected error when trying to set cufflinks isoform FPKM output")
    // cufflinks always outputs a isoforms.fpkm_tracking file in the output directory
    new File(output_dir + File.pathSeparator + "isoforms.fpkm_tracking")
  }

  @Output(doc = "Output GTF file")
  lazy val output_genes_fpkm: File = {
    if (input == null || output_dir == null)
      throw new RuntimeException("Unexpected error when trying to set cufflinks genes FPKM output")
    // cufflinks always outputs a genes.fpkm_tracking file in the output directory
    new File(output_dir + File.pathSeparator + "genes.fpkm_tracking")
  }

  /** write all output files to this directory [./] */
  var output_dir: String = _

  /** number of threads used during analysis [1] */
  var num_threads: Option[Int] = config("num_threads")

  /** value of random number generator seed [0] */
  var seed: Option[Int] = config("seed")

  /** quantitate against reference transcript annotations */
  var GTF: File = config("GTF")

  /** use reference transcript annotation to guide assembly */
  var GTF_guide: File = config("GTF_guide")

  /** ignore all alignment within transcripts in this file */
  var mask_file: File = config("mask_file")

  /** use bias correction - reference fasta required [NULL] */
  var frag_bias_correct: String = config("frag_bias_correct")

  /** use 'rescue method' for multi-reads (more accurate) [FALSE] */
  var multi_read_correct: Boolean = config("multi_read_correct")

  /** library prep used for input reads [below] */
  var library_type: String = config("library_type")

  /** Method used to normalize library sizes [below] */
  var library_norm_method: String = config("library_norm_method")

  /** average fragment length (unpaired reads only) [200] */
  var frag_len_mean: Option[Int] = config("frag_len_mean")

  /** fragment length std deviation (unpaired reads only) [80] */
  var frag_len_std_dev: Option[Int] = config("frag_len_std_dev")

  /** maximum iterations allowed for MLE calculation [5000] */
  var max_mle_iterations: Option[Int] = config("max_mle_iterations")

  /** count hits compatible with reference RNAs only [FALSE] */
  var compatible_hits_norm: Boolean = config("compatible_hits_norm")

  /** count all hits for normalization [TRUE] */
  var total_hits_norm: Boolean = config("total_hits_norm")

  /** Number of fragment generation samples [100] */
  var num_frag_count_draws: Option[Int] = config("num_frag_count_draws")

  /** Number of fragment assignment samples per generation [50] */
  var num_frag_assign_draws: Option[Int] = config("num_frag_assign_draws")

  /** Maximum number of alignments allowed per fragment [unlim] */
  var max_frag_multihits: String = config("max_frag_multihits")

  /** No effective length correction [FALSE] */
  var no_effective_length_correction: Boolean = config("no_effective_length_correction")

  /** No length correction [FALSE] */
  var no_length_correction: Boolean = config("no_length_correction")

  /** assembled transcripts have this ID prefix [CUFF] */
  var label: String = config("label")

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
  var no_faux_reads: Boolean = config("no_faux_reads")

  /** overhang allowed on 3' end when merging with reference [600] */
  var flag_3_overhang_tolerance: Option[Int] = config("flag_3_overhang_tolerance")

  /** overhang allowed inside reference intron when merging [30] */
  var intron_overhang_tolerance: Option[Int] = config("intron_overhang_tolerance")

  /** log-friendly verbose processing (no progress bar) [FALSE] */
  var verbose: Boolean = config("verbose")

  /** log-friendly quiet processing (no progress bar) [FALSE] */
  var quiet: Boolean = config("quiet")

  /** do not contact server to check for update availability [FALSE] */
  var no_update_check: Boolean = config("no_update_check")

  override val versionRegex = """cufflinks v(.*)""".r
  override def versionCommand = executable

  def cmdLine = {
      required(executable) +
      required("--output-dir", output_dir) +
      optional("--num-threads", num_threads) +
      optional("--seed", seed) +
      optional("--GTF", GTF) +
      optional("--GTF-guide", GTF_guide) +
      optional("--mask-file", mask_file) +
      optional("--frag-bias-correct", frag_bias_correct) +
      optional("--multi-read-correct", multi_read_correct) +
      optional("--library-type", library_type) +
      optional("--library-norm-method", library_norm_method) +
      optional("--frag-len-mean", frag_len_mean) +
      optional("--frag-len-std-dev", frag_len_std_dev) +
      optional("--max-mle-iterations", max_mle_iterations) +
      optional("--compatible-hits-norm", compatible_hits_norm) +
      optional("--total-hits-norm", total_hits_norm) +
      optional("--num-frag-count-draws", num_frag_count_draws) +
      optional("--num-frag-assign-draws", num_frag_assign_draws) +
      optional("--max-frag-multihits", max_frag_multihits) +
      optional("--no-effective-length-correction", no_effective_length_correction) +
      optional("--no-length-correction", no_length_correction) +
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
      optional("--no-faux-reads", no_faux_reads) +
      optional("--flag-3-overhang-tolerance", flag_3_overhang_tolerance) +
      optional("--intron-overhang-tolerance", intron_overhang_tolerance) +
      optional("--verbose", verbose) +
      optional("--quiet", quiet) +
      optional("--no-update-check", no_update_check) +
      required(input)
  }
}
