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
  * Wrapper for the htseq-count command line tool
  * Written based on htseq-count version 0.6.1p1
  */
class HtseqCount(val parent: Configurable) extends BiopetCommandLineFunction with Version {

  /** default executable */
  executable = config("exe", default = "htseq-count")

  /** input file */
  @Input(doc = "Input alignment file", required = true)
  var inputAlignment: File = _

  /** input GFF / GTF annotation file */
  @Input(doc = "Input GFF / GTF annotation file", required = true)
  var inputAnnotation: File = _

  /** output file */
  @Output(doc = "Output count file", required = true)
  var output: File = _

  /** type of input alignment */
  var format: Option[String] = config("format")

  /** sorting order of alignment file */
  var order: Option[String] = config("order")

  /** whether alignment is strand specific or not */
  var stranded: Option[String] = config("stranded")

  /** skip all reads with alignment quality lower than the given minimum value */
  var minaqual: Option[Int] = config("minaqual")

  /** feature type to be used */
  var featuretype: Option[String] = config("type")

  /** attribute to use as feature ID */
  var idattr: Option[String] = config("idattr")

  /** counting mode */
  var mode: Option[String] = config("mode")

  /** write all SAM alignment records into an output file */
  @Output(doc = "Optional SAM file output", required = false)
  var samout: Option[File] = None

  /** suppress progress report */
  var quiet: Boolean = config("quiet", default = false)

  def versionRegex: List[Regex] = """.*, version (.*)\.""".r :: Nil
  def versionCommand: String = executable + " --help"

  def cmdLine: String = {
    required(executable) +
      optional("--format", format) +
      optional("--order", order) +
      optional("--stranded", stranded) +
      optional("--minaqual", minaqual) +
      optional("--type", featuretype) +
      optional("--idattr", idattr) +
      optional("--mode", mode) +
      optional("--samout", samout) +
      conditional(quiet, "--quiet") +
      required(inputAlignment) +
      required(inputAnnotation) +
      " > " + required(output)
  }
}
