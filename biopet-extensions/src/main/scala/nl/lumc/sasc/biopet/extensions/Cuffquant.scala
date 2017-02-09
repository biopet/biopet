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

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Wrapper for the cuffquant command line tool.
 * Written based on cuffquant version v2.2.1 (md5: 0765b82b11db9256f5be341a7da884d6)
 */
class Cuffquant(val parent: Configurable) extends BiopetCommandLineFunction with Version {

  /** default executable */
  executable = config("exe", default = "cuffquant")

  /** input file */
  @Input(doc = "Input file (SAM or BAM)", required = true) /*
    in cuffquant this input: sample1_rep1.sam,sample1_rep2.sam sample2_rep1.sam,sample2_rep2.sam
    means we have 2 samples, each with 2 replicates
    so our input is a list of lists of Files
   */
  var input: List[List[File]] = Nil

  /** input GTF file */
  @Input(doc = "Input GTF file", required = true)
  var transcriptsGtf: File = _

  /** output file, computed automatically from output directory */
  @Output(doc = "Output CXB file")
  lazy val outputCxb: File = {
    require(outputDir != null,
      "Can not set Cuffquant CXB output while input file and/or output directory is not defined")
    // cufflinks always outputs a transcripts.gtf file in the output directory
    new File(outputDir, "abundances.cxb")
  }

  /** write all output files to this directory [./] */
  var outputDir: File = config("output_dir", default = new File("."))

  /** ignore all alignment within transcripts in this file */
  var maskFile: Option[File] = config("mask_file")

  /** use bias correction - reference fasta required [NULL] */
  var fragBiasCorrect: Option[String] = config("frag_bias_correct")

  /** use 'rescue method' for multi-reads (more accurate) [FALSE] */
  var multiReadCorrect: Boolean = config("multi_read_correct", default = false)

  /** number of threads used during analysis [1] */
  var numThreads: Option[Int] = config("num_threads")

  /** library prep used for input reads [below] */
  var libraryType: Option[String] = config("library_type")

  /** average fragment length (unpaired reads only) [200] */
  var fragLenMean: Option[Int] = config("frag_len_mean")

  /** fragment length std deviation (unpaired reads only) [80] */
  var fragLenStdDev: Option[Int] = config("frag_len_std_dev")

  /** minimum number of alignments in a locus for testing [10] */
  var minAlignmentCount: Option[Int] = config("min_alignment_count")

  /** maximum iterations allowed for MLE calculation [5000] */
  var maxMleIterations: Option[Int] = config("max_mle_iterations")

  /** log-friendly verbose processing (no progress bar) [FALSE] */
  var verbose: Boolean = config("verbose", default = false)

  /** log-friendly quiet processing (no progress bar) [FALSE] */
  var quiet: Boolean = config("quiet", default = false)

  /** value of random number generator seed [0] */
  var seed: Option[Int] = config("seed")

  /** do not contact server to check for update availability [FALSE] */
  var noUpdateCheck: Boolean = config("no_update_check", default = false)

  /** maximum fragments allowed in a bundle before skipping [500000] */
  var maxBundleFrags: Option[Int] = config("max_bundle_frags")

  /** Maximum number of alignments allowed per fragment [unlim] */
  var maxFragMultihits: Option[Int] = config("max_frag_multihits")

  /** No effective length correction [FALSE] */
  var noEffectiveLengthCorrection: Boolean = config("no_effective_length_correction", default = false)

  /** No length correction [FALSE] */
  var noLengthCorrection: Boolean = config("no_length_correction", default = false)

  /** Skip a random subset of reads this size [0.0] */
  var readSkipFraction: Option[Double] = config("read_skip_fraction")

  /** Break all read pairs [FALSE] */
  var noReadPairs: Boolean = config("no_read_pairs", default = false)

  /** Trim reads to be this long (keep 5' end) [none] */
  var trimReadLength: Option[Int] = config("trim_read_length")

  /** Disable SCV correction */
  var noScvCorrection: Boolean = config("no_scv_correction", default = false)

  def versionRegex = """cuffquant v(.*)""".r
  def versionCommand = executable
  override def versionExitcode = List(0, 1)

  def cmdLine =
    required(executable) +
      required("--output-dir", outputDir) +
      optional("--mask-file", maskFile) +
      optional("--frag-bias-correct", fragBiasCorrect) +
      conditional(multiReadCorrect, "--multi-read-correct") +
      optional("--num-threads", numThreads) +
      optional("--library-type", libraryType) +
      optional("--frag-len-mean", fragLenMean) +
      optional("--frag-len-std-dev", fragLenStdDev) +
      optional("--min-alignment-count", minAlignmentCount) +
      optional("--max-mle-iterations", maxMleIterations) +
      conditional(verbose, "--verbose") +
      conditional(quiet, "--quiet") +
      optional("--seed", seed) +
      conditional(noUpdateCheck, "--no-update-check") +
      optional("--max-bundle-frags", maxBundleFrags) +
      optional("--max-frag-multihits", maxFragMultihits) +
      conditional(noEffectiveLengthCorrection, "--no-effective-length-correction") +
      conditional(noLengthCorrection, "--no-length-correction") +
      optional("--read-skip-fraction", readSkipFraction) +
      conditional(noReadPairs, "--no-read-pairs") +
      optional("--trim-read-length", trimReadLength) +
      conditional(noScvCorrection, "--no-scv-correction") +
      required(transcriptsGtf) +
      required(input.map(_.mkString(";").mkString(" ")))
}
