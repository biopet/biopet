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

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.summary.Summarizable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.collection.mutable
import scala.io.Source

/**
 * Extension for cutadapt
 * Started with version 1.5
 * Updated to version 1.9 (18-01-2016 by wyleung)
 */
class Cutadapt(val root: Configurable) extends BiopetCommandLineFunction with Summarizable with Version {
  @Input(doc = "Input fastq file")
  var fastqInput: File = _

  @Output
  var fastqOutput: File = _

  @Output(doc = "Output statistics file")
  var statsOutput: File = _

  executable = config("exe", default = "cutadapt")
  def versionCommand = executable + " --version"
  def versionRegex = """(.*)""".r

  /** Name of the key containing clipped adapters information in the summary stats. */
  def adaptersStatsName = "adapters"

  var defaultClipMode: String = config("default_clip_mode", default = "3")
  var adapter: Set[String] = config("adapter", default = Nil)
  var anywhere: Set[String] = config("anywhere", default = Nil)
  var front: Set[String] = config("front", default = Nil)

  var errorRate: Option[Double] = config("error_rate")
  var noIndels: Boolean = config("no_indels", default = false)
  var times: Option[Int] = config("times")
  var overlap: Option[Int] = config("overlap")
  var matchReadWildcards: Boolean = config("match_read_wildcards", default = false)
  var noMatchAdapterWildcards: Boolean = config("no_match_adapter_wildcards", default = false) // specific for 1.9

  /** Options for filtering of processed reads */
  var discard: Boolean = config("discard", default = false)
  var trimmedOnly: Boolean = config("trimmed_only", default = false)
  var minimumLength: Int = config("minimum_length", 1)
  var maximumLength: Option[Int] = config("maximum_length")
  var noTrim: Boolean = config("no_trim", default = false)
  var maxN: Option[Int] = config("max_n") // specific for 1.9
  var maskAdapter: Boolean = config("mask_adapter", default = false)

  /** Options that influence what gets output to where */
  var quiet: Boolean = config("quiet", default = false)
  //  var output: File // see up @Output
  var infoFile: Option[File] = config("info_file")
  var restFile: Option[File] = config("rest_file")
  var wildcardFile: Option[File] = config("wildcard_file")
  var tooShortOutput: Option[File] = config("too_short_output")
  var tooLongOutput: Option[File] = config("too_long_output")
  var untrimmedOutput: Option[File] = config("untrimmed_output")

  /** Additional read modifications */
  var cut: Option[Int] = config("cut")
  var qualityCutoff: Option[String] = config("quality_cutoff")
  var qualityBase: Option[Int] = config("quality_base")
  var trimN: Boolean = config("trim_n", default = false)
  var prefix: Option[String] = config("prefix")
  var suffix: Option[String] = config("suffix")
  var stripSuffix: Set[String] = config("strip_suffix")
  var lengthTag: Option[String] = config("length_tag")

  /** Colorspace options */
  var colorspace: Boolean = config("colorspace", default = false)
  var doubleEncode: Boolean = config("double_encode", default = false)
  var trimPrimer: Boolean = config("trim_primer", default = false)
  var stripF3: Boolean = config("strip_f3", default = false)
  var maq: Boolean = config("maq", default = false)
  var bwa: Boolean = config("bwa", default = false)
  var noZeroCap: Boolean = config("no_zero_cap", default = false)
  var zeroCap: Boolean = config("zero_cap", default = false)

  /** Paired end options */
  var peAdapter: Set[String] = config("pe_adapter", default = Nil)
  var peAdapterFront: Set[String] = config("pe_adapter_front", default = Nil)
  var peAdapterBoth: Set[String] = config("pe_adapter_both", default = Nil)
  var peCut: Boolean = config("pe_cut", default = false)
  var pairedOutput: Option[File] = config("paired_output")
  var interleaved: Boolean = config("interleaved", default = false)
  var untrimmedPairedOutput: Option[File] = config("untrimmed_paired_output")

  /** return commandline to execute */
  def cmdLine = required(executable) +
    // Options that influence how the adapters are found
    repeat("-a", adapter) +
    repeat("-b", anywhere) +
    repeat("-g", front) +
    optional("--error-rate", errorRate) +
    conditional(noIndels, "--no-indels") +
    optional("--times", times) +
    optional("--overlap", overlap) +
    conditional(matchReadWildcards, "--match-read-wildcards") +
    conditional(noMatchAdapterWildcards, "--no-match-adapter-wildcards") +
    // Options for filtering of processed reads
    conditional(discard, "--discard") +
    conditional(trimmedOnly, "--trimmed-only") +
    optional("-m", minimumLength) +
    optional("-M", maximumLength) +
    conditional(noTrim, "--no-trim") +
    optional("--max-n", maxN) +
    conditional(maskAdapter, "--mask-adapter") +
    conditional(quiet, "--quiet") +
    optional("--info-file", infoFile) +
    optional("--rest-file", restFile) +
    optional("--wildcard-file", wildcardFile) +
    optional("--too-short-output", tooShortOutput) +
    optional("--too-long-output", tooLongOutput) +
    optional("--untrimmed-output", untrimmedOutput) +
    // Additional read modifications
    optional("--cut", cut) +
    optional("--quality-cutoff", qualityCutoff) +
    conditional(trimN, "--trim-n") +
    optional("--prefix", prefix) +
    optional("--suffix", suffix) +
    optional("--strip-suffix", stripSuffix) +
    optional("--length-tag", lengthTag) +
    // Colorspace options
    conditional(colorspace, "--colorspace") +
    conditional(doubleEncode, "--double-encode") +
    conditional(trimPrimer, "--trim-primer") +
    conditional(stripF3, "--strip-f3") +
    conditional(maq, "--maq") +
    conditional(bwa, "--bwa") +
    conditional(noZeroCap, "--no-zero-cap") +
    conditional(zeroCap, "--zero-cap") +
    // Paired-end options
    repeat("-A", peAdapter) +
    repeat("-G", peAdapterFront) +
    repeat("-B", peAdapterBoth) +
    conditional(interleaved, "--interleaved") +
    optional("--paired-output", pairedOutput) +
    optional("--untrimmed-paired-output", untrimmedPairedOutput) +
    // input / output
    required(fastqInput) +
    (if (outputAsStsout) "" else required("--output", fastqOutput) +
      " > " + required(statsOutput))

  /** Output summary stats */
  def summaryStats: Map[String, Any] = {
    val trimR = """.*Trimmed reads: *(\d*) .*""".r
    val tooShortR = """.*Too short reads: *(\d*) .*""".r
    val tooLongR = """.*Too long reads: *(\d*) .*""".r
    val adapterR = """Adapter '([C|T|A|G]*)'.*trimmed (\d*) times.""".r

    val stats: mutable.Map[String, Int] = mutable.Map("trimmed" -> 0, "tooshort" -> 0, "toolong" -> 0)
    val adapter_stats: mutable.Map[String, Int] = mutable.Map()

    if (statsOutput.exists) for (line <- Source.fromFile(statsOutput).getLines()) {
      line match {
        case trimR(m)                 => stats += ("trimmed" -> m.toInt)
        case tooShortR(m)             => stats += ("tooshort" -> m.toInt)
        case tooLongR(m)              => stats += ("toolong" -> m.toInt)
        case adapterR(adapter, count) => adapter_stats += (adapter -> count.toInt)
        case _                        =>
      }
    }

    Map("num_reads_affected" -> stats("trimmed"),
      "num_reads_discarded_too_short" -> stats("tooshort"),
      "num_reads_discarded_too_long" -> stats("toolong"),
      adaptersStatsName -> adapter_stats.toMap
    )
  }

  /** Merges values that can be merged for the summary */
  override def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    (v1, v2) match {
      case (v1: Int, v2: Int) => v1 + v2
      case _                  => v1
    }
  }

  def summaryFiles: Map[String, File] = Map()
}
