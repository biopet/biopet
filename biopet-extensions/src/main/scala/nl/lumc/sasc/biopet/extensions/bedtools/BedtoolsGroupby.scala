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
package nl.lumc.sasc.biopet.extensions.bedtools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * Wrapper for the bedtools groupby tool
  * Written based on bedtools v2.21.0 (md5: b5a9a64bad721d96f6cbf2b3805b0fbe)
  *
  * @param parent [[Configurable]] object
  */
class BedtoolsGroupby(val parent: Configurable) extends Bedtools {

  // input can be from stdin or a file
  @Input(doc = "Input file", required = false)
  var input: Option[File] = None

  // output can be to stdout or a file
  @Output(doc = "Output file", required = false)
  var output: Option[File] = None

  @Argument(doc = "Columns used for grouping (1-based)", required = false)
  var grp: List[Int] = List.empty[Int]

  @Argument(doc = "Columns that will be summarized (1-based)")
  var opCols: List[Int] = List.empty[Int]

  @Argument(doc = "Operations that will be applied to summarized column")
  var ops: List[String] = List.empty[String]

  @Argument(doc = "Whether to print all columns from input file or not", required = false)
  var full: Boolean = false

  @Argument(doc = "Input file has a header - the first line will be ignored", required = false)
  var inheader: Boolean = false

  @Argument(doc = "Print header in the output file", required = false)
  var outheader: Boolean = false

  @Argument(doc = "Set -inheader and -outheader", required = false)
  var header: Boolean = false

  @Argument(doc = "Group values regardless of upper/lower case", required = false)
  var ignorecase: Boolean = false

  @Argument(doc = "Set decimal precision for output", required = false)
  var prec: Option[Int] = None

  /** Input flag of the file */
  def inputFlag: String =
    if (input.nonEmpty) "-i"
    else ""

  /** Output flag of the file */
  def outputFlag: String =
    if (output.nonEmpty) " > "
    else ""

  def cmdLine =
    required(executable) + required("groupby") +
      optional(inputFlag, input) +
      optional("-g", grp) +
      required("-c", opCols) +
      required("-o", ops) +
      conditional(full, "-full") +
      conditional(inheader, "-inheader") +
      conditional(outheader, "-outheader") +
      conditional(header, "-header") +
      conditional(ignorecase, "-ignorecase") +
      optional("-prec", prec) +
      optional(outputFlag, output)
}
