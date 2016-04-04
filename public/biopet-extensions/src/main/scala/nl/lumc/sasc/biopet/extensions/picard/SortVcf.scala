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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/** Extension for picard SortVcf */
class SortVcf(val root: Configurable) extends Picard {
  javaMainClass = new picard.vcf.SortVcf().getClass.getName

  @Input(doc = "Input VCF(s) to be sorted. Multiple inputs must have the same sample names (in order)", required = true)
  var input: File = _

  @Output(doc = "Output VCF to be written.", required = true)
  var output: File = _

  @Argument(doc = "Sequence dictionary to use", required = false)
  var sequenceDictionary: String = config("sort_order", default = "coordinate")

  /** Returns command to execute */
  override def cmdLine = super.cmdLine +
    (if (inputAsStdin) required("INPUT=", new File("/dev/stdin"), spaceSeparated = false)
    else required("INPUT=", input, spaceSeparated = false)) +
    (if (outputAsStsout) required("OUTPUT=", new File("/dev/stdout"), spaceSeparated = false)
    else required("OUTPUT=", output, spaceSeparated = false)) +
    required("SEQUENCE_DICTIONARY=", sequenceDictionary, spaceSeparated = false)
}

object SortVcf {
  /** Returns default SortSam */
  def apply(root: Configurable, input: File, output: File): SortVcf = {
    val sortVcf = new SortVcf(root)
    sortVcf.input = input
    sortVcf.output = output
    sortVcf
  }
}