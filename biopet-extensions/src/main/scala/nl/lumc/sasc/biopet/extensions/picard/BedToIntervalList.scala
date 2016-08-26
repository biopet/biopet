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

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Extension for piacrd's BedToIntervalList tool
 *
 * Created by pjvan_thof on 4/15/15.
 */
class BedToIntervalList(val root: Configurable) extends Picard with Reference {
  javaMainClass = new picard.util.BedToIntervalList().getClass.getName

  @Input(doc = "Input bed file", required = true)
  var input: File = null

  @Input(doc = "Reference dict file", required = true)
  var dict: File = new File(referenceFasta().toString.stripSuffix(".fa").stripSuffix(".fasta") + ".dict")

  @Output(doc = "Output interval list", required = true)
  var output: File = null

  override def cmdLine = super.cmdLine +
    required("SEQUENCE_DICTIONARY=", dict, spaceSeparated = false) +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false)
}

object BedToIntervalList {
  def apply(root: Configurable, input: File, output: File): BedToIntervalList = {
    val bi = new BedToIntervalList(root)
    bi.input = input
    bi.output = output
    bi
  }
}