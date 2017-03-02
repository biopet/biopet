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
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/** Extension for picard SortSam */
class SortSam(val parent: Configurable) extends Picard {
  javaMainClass = new picard.sam.SortSam().getClass.getName

  @Input(doc = "The input SAM or BAM files to analyze.", required = true)
  var input: File = _

  @Output(doc = "The output file to bam file to", required = true)
  var output: File = _

  @Argument(doc = "Sort order of output file Required. Possible values: {unsorted, queryname, coordinate} ", required = true)
  var sortOrder: String = config("sort_order", default = "coordinate")

  @Output(doc = "Bam Index", required = true)
  private var outputIndex: File = _

  override def beforeGraph() {
    super.beforeGraph()
    if (outputAsStsout) createIndex = false
    if (createIndex) outputIndex = new File(output.getAbsolutePath.stripSuffix(".bam") + ".bai")
  }

  /** Returns command to execute */
  override def cmdLine = super.cmdLine +
    (if (inputAsStdin) required("INPUT=", new File("/dev/stdin"), spaceSeparated = false)
    else required("INPUT=", input, spaceSeparated = false)) +
    (if (outputAsStsout) required("OUTPUT=", new File("/dev/stdout"), spaceSeparated = false)
    else required("OUTPUT=", output, spaceSeparated = false)) +
    required("SORT_ORDER=", sortOrder, spaceSeparated = false)
}

object SortSam {
  /** Returns default SortSam */
  def apply(root: Configurable, input: File, output: File): SortSam = {
    val sortSam = new SortSam(root)
    sortSam.input = input
    sortSam.output = output
    sortSam
  }
}