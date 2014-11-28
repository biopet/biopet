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
 * Wrapper for the htseq-count command line tool
 * Written based on htseq-count version 0.6.1p1
 */
class HtseqCount(val root: Configurable) extends BiopetCommandLineFunction {

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
  var format: String = config("format")

  /** sorting order of alignment file */
  var order: String = config("order")

  /** whether alignment is strand specific or not */
  var stranded: String = config("stranded")

  /** skip all reads with alignment quality lower than the given minimum value */
  var minaqual: Option[Int] = config("minaqual")

  /** feature type to be used */
  var featuretype: String = config("type")

  /** attribute to use as feature ID */
  var idattr: String = config("idattr")

  /** counting mode */
  var mode: String = config("mode")

  /** write all SAM alignment records into an output file */
  @Output(doc = "Optional SAM file output", required = false)
  var samout: File = _

  /** suppress progress report */
  var quiet: Boolean = config("quiet")

  override val versionRegex = """.*, version (.*)\.""".r
  override def versionCommand = executable + " --help"

  def cmdLine = {
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
