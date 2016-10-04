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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by sajvanderzeeuw on 6-10-15.
 */
class NormalizeFasta(val root: Configurable) extends Picard {

  javaMainClass = new picard.reference.NormalizeFasta().getClass.getName

  @Input(doc = "The input fasta file", required = true)
  var input: File = _

  @Output(doc = "The output fasta file", required = true)
  var output: File = _

  val lineLength: Int = config("line_length")

  val truncateSequenceNameAtWhitespace: Boolean = config("truncate_sequence_name_at_whitespace", default = false)

  override def cmdLine = super.cmdLine +
    (if (inputAsStdin) required("INPUT=", new File("/dev/stdin"), spaceSeparated = false)
    else required("INPUT=", input, spaceSeparated = false)) +
    (if (outputAsStsout) required("OUTPUT=", new File("/dev/stdout"), spaceSeparated = false)
    else required("OUTPUT=", output, spaceSeparated = false)) +
    required("LINE_LENGTH=", output, spaceSeparated = false) +
    conditional(truncateSequenceNameAtWhitespace, "TRUNCATE_SEQUENCE_NAMES_AT_WHITESPACE=TRUE")
}
