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

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

/**
  * Wrapper for the cufflinks command line tool.
  * Written based on cufflinks version v2.2.1 (md5: 07c831c4f8b4e161882731ea5694ff80)
  */
class Cufflinks(val parent: Configurable) extends BiopetCommandLineFunction with Version {

  /** default executable */
  executable = config("exe", default = "cufflinks")

  /** default threads */
  override def defaultThreads = 8

  /** default vmem for cluster jobs */
  override def defaultCoreMemory = 6.0

  /** input file */
  @Input(doc = "Input file (SAM or BAM)", required = true)
  var input: File = _

  /** output files, computed automatically from output directory */
  @Output(doc = "Output GTF file")
  lazy val outputGtf: File = {
    require(
      input != null && outputDir != null,
      "Can not set Cufflinks GTF output while input file and/or output directory is not defined")
    // cufflinks always outputs a transcripts.gtf file in the output directory
    new File(outputDir, "transcripts.gtf")
  }

  @Output(doc = "Output isoform FPKM file")
  lazy val outputIsoformsFpkm: File = {
    require(
      input != null && outputDir != null,
      "Can not set Cufflinks isoforms.fpkm_tracking output while input file and/or output directory is not defined")
    new File(outputDir, "isoforms.fpkm_tracking")
  }

  @Output(doc = "Output GTF file")
  lazy val outputGenesFpkm: File = {
    require(
      input != null && outputDir != null,
      "Can not set Cufflinks genes.fpkm_tracking output while input file and/or output directory is not defined")
    // cufflinks always outputs a genes.fpkm_tracking file in the output directory
    new File(outputDir, "genes.fpkm_tracking")
  }

  /** write all output files to this directory [./] */
  var outputDir: File = config("output_dir", default = new File("."))

  /** value of random number generator seed [0] */
  var seed: Option[Int] = config("seed")

  /** quantitate against reference transcript annotations */
  var GTF: Option[File] = config("GTF")

  /** use reference transcript annotation to guide assembly */
  var gtfGuide: Option[File] = config("GTF_guide")

  /** ignore all alignment within transcripts in this file */
  var maskFile: Option[File] = config("mask_file")

  /** use bias correction - reference fasta required [NULL] */
  var fragBiasCorrect: Option[String] = config("frag_bias_correct")

  /** use 'rescue method' for multi-reads (more accurate) [FALSE] */
  var multiReadCorrect: Boolean = config("multi_read_correct", default = false)

  /** library prep used for input reads [below] */
  var libraryType: Option[String] = config("library_type")

  /** Method used to normalize library sizes [below] */
  var libraryNormMethod: Option[String] = config("library_norm_method")

  /** average fragment length (unpaired reads only) [200] */
  var fragLenMean: Option[Int] = config("frag_len_mean")

  /** fragment length std deviation (unpaired reads only) [80] */
  var fragLenStdDev: Option[Int] = config("frag_len_std_dev")

  /** maximum iterations allowed for MLE calculation [5000] */
  var maxMleIterations: Option[Int] = config("max_mle_iterations")

  /** count hits compatible with reference RNAs only [FALSE] */
  var compatibleHitsNorm: Boolean = config("compatible_hits_norm", default = false)

  /** count all hits for normalization [TRUE] */
  var totalHitsNorm: Boolean = config("total_hits_norm", default = true)

  /** Number of fragment generation samples [100] */
  var numFragCountDraws: Option[Int] = config("num_frag_count_draws")

  /** Number of fragment assignment samples per generation [50] */
  var numFragAssignDraws: Option[Int] = config("num_frag_assign_draws")

  /** Maximum number of alignments allowed per fragment [unlim] */
  var maxFragMultihits: Option[Int] = config("max_frag_multihits")

  /** No effective length correction [FALSE] */
  var noEffectiveLengthCorrection: Boolean =
    config("no_effective_length_correction", default = false)

  /** No length correction [FALSE] */
  var noLengthCorrection: Boolean = config("no_length_correction", default = false)

  /** assembled transcripts have this ID prefix [CUFF] */
  var label: Option[String] = config("label")

  /** suppress transcripts below this abundance level [0.10] */
  var minIsoformFraction: Option[Float] = config("min_isoform_fraction")

  /** suppress intra-intronic transcripts below this level [0.15] */
  var preMrnaFraction: Option[Float] = config("pre_mrna_fraction")

  /** ignore alignments with gaps longer than this [300000] */
  var maxIntronLength: Option[Int] = config("max_intron_length")

  /** alpha for junction binomial test filter [0.001] */
  var juncAlpha: Option[Float] = config("junc_alpha")

  /** percent read overhang taken as 'suspiciously small' [0.09] */
  var smallAnchorFraction: Option[Float] = config("small_anchor_fraction")

  /** minimum number of fragments needed for new transfrags [10] */
  var minFragsPerTransfrag: Option[Int] = config("min_frags_per_transfrag")

  /** number of terminal exon bp to tolerate in introns [8] */
  var overhangTolerance: Option[Int] = config("overhang_tolerance")

  /** maximum genomic length allowed for a given bundle [3500000] */
  var maxBundleLength: Option[Int] = config("max_bundle_length")

  /** maximum fragments allowed in a bundle before skipping [500000] */
  var maxBundleFrags: Option[Int] = config("max_bundle_frags")

  /** minimum intron size allowed in genome [50] */
  var minIntronLength: Option[Int] = config("min_intron_length")

  /** minimum avg coverage required to attempt 3' trimming [10] */
  var trim3AvgCovThresh: Option[Int] = config("trim_3_avgcov_thresh")

  /** fraction of avg coverage below which to trim 3' end [0.1] */
  var trim3DropOffFrac: Option[Float] = config("trim_3_dropoff_frac")

  /** maximum fraction of allowed multireads per transcript [0.75] */
  var maxMultireadFraction: Option[Float] = config("max_multiread_fraction")

  /** maximum gap size to fill between transfrags (in bp) [50] */
  var overlapRadius: Option[Int] = config("overlap_radius")

  /** disable tiling by faux reads [FALSE] */
  var noFauxReads: Boolean = config("no_faux_reads", default = false)

  /** overhang allowed on 3' end when merging with reference [600] */
  var flag3OverhangTolerance: Option[Int] = config("flag_3_overhang_tolerance")

  /** overhang allowed inside reference intron when merging [30] */
  var intronOverhangTolerance: Option[Int] = config("intron_overhang_tolerance")

  /** log-friendly verbose processing (no progress bar) [FALSE] */
  var verbose: Boolean = config("verbose", default = false)

  /** log-friendly quiet processing (no progress bar) [FALSE] */
  var quiet: Boolean = config("quiet", default = false)

  /** do not contact server to check for update availability [FALSE] */
  var noUpdateCheck: Boolean = config("no_update_check", default = false)

  def versionRegex: List[Regex] = """cufflinks v(.*)""".r :: Nil
  def versionCommand: String = executable
  override def versionExitcode = List(0, 1)

  def cmdLine: String =
    required(executable) +
      required("--output-dir", outputDir) +
      optional("--num-threads", threads) +
      optional("--seed", seed) +
      optional("--GTF", GTF) +
      optional("--GTF-guide", gtfGuide) +
      optional("--mask-file", maskFile) +
      optional("--frag-bias-correct", fragBiasCorrect) +
      conditional(multiReadCorrect, "--multi-read-correct") +
      optional("--library-type", libraryType) +
      optional("--library-norm-method", libraryNormMethod) +
      optional("--frag-len-mean", fragLenMean) +
      optional("--frag-len-std-dev", fragLenStdDev) +
      optional("--max-mle-iterations", maxMleIterations) +
      conditional(compatibleHitsNorm, "--compatible-hits-norm") +
      conditional(totalHitsNorm, "--total-hits-norm") +
      optional("--num-frag-count-draws", numFragCountDraws) +
      optional("--num-frag-assign-draws", numFragAssignDraws) +
      optional("--max-frag-multihits", maxFragMultihits) +
      conditional(noEffectiveLengthCorrection, "--no-effective-length-correction") +
      conditional(noLengthCorrection, "--no-length-correction") +
      optional("--label", label) +
      optional("--min-isoform-fraction", minIsoformFraction) +
      optional("--pre-mrna-fraction", preMrnaFraction) +
      optional("--max-intron-length", maxIntronLength) +
      optional("--junc-alpha", juncAlpha) +
      optional("--small-anchor-fraction", smallAnchorFraction) +
      optional("--min-frags-per-transfrag", minFragsPerTransfrag) +
      optional("--overhang-tolerance", overhangTolerance) +
      optional("--max-bundle-length", maxBundleLength) +
      optional("--max-bundle-frags", maxBundleFrags) +
      optional("--min-intron-length", minIntronLength) +
      optional("--trim-3-avgcov-thresh", trim3AvgCovThresh) +
      optional("--trim-3-dropoff-frac", trim3DropOffFrac) +
      optional("--max-multiread-fraction", maxMultireadFraction) +
      optional("--overlap-radius", overlapRadius) +
      conditional(noFauxReads, "--no-faux-reads") +
      optional("--flag-3-overhang-tolerance", flag3OverhangTolerance) +
      optional("--intron-overhang-tolerance", intronOverhangTolerance) +
      conditional(verbose, "--verbose") +
      conditional(quiet, "--quiet") +
      conditional(noUpdateCheck, "--no-update-check") +
      required(input)
}
