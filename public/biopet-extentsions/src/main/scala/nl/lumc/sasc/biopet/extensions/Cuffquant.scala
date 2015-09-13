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
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Wrapper for the cuffquant command line tool.
 * Written based on cuffquant version v2.2.1 (md5: 0765b82b11db9256f5be341a7da884d6)
 */
class Cuffquant(val root: Configurable) extends BiopetCommandLineFunction {

  /** default executable */
  executable = config("exe", default = "cuffquant")

  /** input file */
  @Input(doc = "Input file (SAM or BAM)", required = true) /*
    in cuffquant this input: sample1_rep1.sam,sample1_rep2.sam sample2_rep1.sam,sample2_rep2.sam
    means we have 2 samples, each with 2 replicates
    so our input is a list of lists of Files
   */
  var input: List[List[File]] = List.empty[List[File]]

  /** input GTF file */
  @Input(doc = "Input GTF file", required = true)
  var transcripts_gtf: File = null

  /** output file, computed automatically from output directory */
  @Output(doc = "Output CXB file")
  lazy val outputCxb: File = {
    require(output_dir != null,
      "Can not set Cuffquant CXB output while input file and/or output directory is not defined")
    // cufflinks always outputs a transcripts.gtf file in the output directory
    new File(output_dir, "abundances.cxb")
  }

  /** write all output files to this directory [./] */
  var output_dir: File = config("output_dir", default = new File("."))

  /** ignore all alignment within transcripts in this file */
  var mask_file: Option[File] = config("mask_file")

  /** use bias correction - reference fasta required [NULL] */
  var frag_bias_correct: Option[String] = config("frag_bias_correct")

  /** use 'rescue method' for multi-reads (more accurate) [FALSE] */
  var multi_read_correct: Boolean = config("multi_read_correct", default = false)

  /** number of threads used during analysis [1] */
  var num_threads: Option[Int] = config("num_threads")

  /** library prep used for input reads [below] */
  var library_type: Option[String] = config("library_type")

  /** average fragment length (unpaired reads only) [200] */
  var frag_len_mean: Option[Int] = config("frag_len_mean")

  /** fragment length std deviation (unpaired reads only) [80] */
  var frag_len_std_dev: Option[Int] = config("frag_len_std_dev")

  /** minimum number of alignments in a locus for testing [10] */
  var min_alignment_count: Option[Int] = config("min_alignment_count")

  /** maximum iterations allowed for MLE calculation [5000] */
  var max_mle_iterations: Option[Int] = config("max_mle_iterations")

  /** log-friendly verbose processing (no progress bar) [FALSE] */
  var verbose: Boolean = config("verbose", default = false)

  /** log-friendly quiet processing (no progress bar) [FALSE] */
  var quiet: Boolean = config("quiet", default = false)

  /** value of random number generator seed [0] */
  var seed: Option[Int] = config("seed")

  /** do not contact server to check for update availability [FALSE] */
  var no_update_check: Boolean = config("no_update_check", default = false)

  /** maximum fragments allowed in a bundle before skipping [500000] */
  var max_bundle_frags: Option[Int] = config("max_bundle_frags")

  /** Maximum number of alignments allowed per fragment [unlim] */
  var max_frag_multihits: Option[Int] = config("max_frag_multihits")

  /** No effective length correction [FALSE] */
  var no_effective_length_correction: Boolean = config("no_effective_length_correction", default = false)

  /** No length correction [FALSE] */
  var no_length_correction: Boolean = config("no_length_correction", default = false)

  /** Skip a random subset of reads this size [0.0] */
  var read_skip_fraction: Option[Double] = config("read_skip_fraction")

  /** Break all read pairs [FALSE] */
  var no_read_pairs: Boolean = config("no_read_pairs", default = false)

  /** Trim reads to be this long (keep 5' end) [none] */
  var trim_read_length: Option[Int] = config("trim_read_length")

  /** Disable SCV correction */
  var no_scv_correction: Boolean = config("no_scv_correction", default = false)

  override def versionRegex = """cuffquant v(.*)""".r
  override def versionCommand = executable
  override def versionExitcode = List(0, 1)

  def cmdLine =
    required(executable) +
      required("--output-dir", output_dir) +
      optional("--mask-file", mask_file) +
      optional("--frag-bias-correct", frag_bias_correct) +
      conditional(multi_read_correct, "--multi-read-correct") +
      optional("--num-threads", num_threads) +
      optional("--library-type", library_type) +
      optional("--frag-len-mean", frag_len_mean) +
      optional("--frag-len-std-dev", frag_len_std_dev) +
      optional("--min-alignment-count", min_alignment_count) +
      optional("--max-mle-iterations", max_mle_iterations) +
      conditional(verbose, "--verbose") +
      conditional(quiet, "--quiet") +
      optional("--seed", seed) +
      conditional(no_update_check, "--no-update-check") +
      optional("--max-bundle-frags", max_bundle_frags) +
      optional("--max-frag-multihits", max_frag_multihits) +
      conditional(no_effective_length_correction, "--no-effective-length-correction") +
      conditional(no_length_correction, "--no-length-correction") +
      optional("--read-skip-fraction", read_skip_fraction) +
      conditional(no_read_pairs, "--no-read-pairs") +
      optional("--trim-read-length", trim_read_length) +
      conditional(no_scv_correction, "--no-scv-correction") +
      required(transcripts_gtf) +
      required(input.map(_.mkString(";").mkString(" ")))
}
