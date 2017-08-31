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
  * Created by pjvanthof on 16/12/15.
  */
class Flash(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "flash", freeVar = false)

  /** Command to get version of executable */
  def versionCommand: String = executable + " --version"

  /** Regex to get version from version command output */
  def versionRegex: List[Regex] = """FLASH (v.*)""".r :: Nil

  @Input(required = true)
  var fastqR1: File = _

  @Input(required = true)
  var fastqR2: File = _

  var minOverlap: Option[Int] = config("min_overlap")
  var maxOverlap: Option[Int] = config("max_overlap")
  var maxMismatchDensity: Option[Double] = config("max_mismatch_density")
  var allowOuties: Boolean = config("allow_outies", default = false)
  var phredOffset: Option[Int] = config("phred_offset")
  var readLen: Option[Int] = config("read_len")
  var fragmentLen: Option[Int] = config("fragment_len")
  var fragmentLenStddev: Option[Int] = config("fragment_len_stddev")
  var capMismatchQuals: Boolean = config("cap_mismatch_quals", default = false)
  var interleavedInput: Boolean = config("interleaved-input", default = false)
  var interleavedOutput: Boolean = config("interleaved_output", default = false)
  var interleaved: Boolean = config("interleaved", default = false)
  var tabDelimitedInput: Boolean = config("tab_delimited_input", default = false)
  var tabDelimitedOutput: Boolean = config("tab_delimited_output", default = false)
  var outputPrefix: String = config("output_prefix", default = "out")
  var outputDirectory: File = _
  var compress: Boolean = config("compress", default = false)
  var compressProg: Option[String] = config("compress_prog")
  var compressProgArgs: Option[String] = config("compress_prog_args")
  var outputSuffix: Option[String] = config("output_suffix")

  private def suffix = outputSuffix.getOrElse("fastq") + (if (compress) ".gz" else "")

  @Output
  private var _combinedFastq: File = _
  def combinedFastq = new File(outputDirectory, s"$outputPrefix.extendedFrags.$suffix")

  @Output
  private var _notCombinedR1: File = _
  def notCombinedR1 = new File(outputDirectory, s"$outputPrefix.notCombined_1.$suffix")

  @Output
  private var _notCombinedR2: File = _
  def notCombinedR2 = new File(outputDirectory, s"$outputPrefix.notCombined_2.$suffix")

  @Output
  private var _outputHistogramTable: File = _
  def outputHistogramTable = new File(outputDirectory, s"$outputPrefix.hist")

  @Output
  private var _outputHistogram: File = _
  def outputHistogram = new File(outputDirectory, s"$outputPrefix.histogram")

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    _combinedFastq = combinedFastq
    _notCombinedR1 = notCombinedR1
    _notCombinedR2 = notCombinedR2
    _outputHistogramTable = outputHistogramTable
    _outputHistogram = outputHistogram
  }

  def cmdLine: String =
    executable +
      optional("-m", minOverlap) +
      optional("-M", maxOverlap) +
      optional("-x", maxMismatchDensity) +
      conditional(allowOuties, "--allow-outies") +
      optional("--phred-offset", phredOffset) +
      optional("--read-len", readLen) +
      optional("--fragment-len", fragmentLen) +
      optional("--fragment-len-stddev", fragmentLenStddev) +
      conditional(capMismatchQuals, "--cap-mismatch-quals") +
      conditional(interleavedInput, "--interleaved-input") +
      conditional(interleavedOutput, "--interleaved-output") +
      conditional(interleaved, "--interleaved") +
      conditional(tabDelimitedInput, "--tab-delimited-input") +
      conditional(tabDelimitedOutput, "--tab-delimited-output") +
      optional("--output-prefix", outputPrefix) +
      required("--output-directory", outputDirectory) +
      conditional(compress, "--compress") +
      optional("--compress-prog", compressProg) +
      optional("--compress-prog-args", compressProgArgs) +
      optional("--output-suffix", outputSuffix) +
      optional("--threads", threads) +
      required(fastqR1) +
      required(fastqR2)
}
