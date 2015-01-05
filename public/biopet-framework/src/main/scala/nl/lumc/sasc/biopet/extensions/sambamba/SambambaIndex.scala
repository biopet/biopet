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
package nl.lumc.sasc.biopet.extensions.sambamba

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class SambambaIndex(val root: Configurable) extends Sambamba {
  override val defaultThreads = 2

  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "Output .bai file to")
  var output: File = _

  def cmdLine = required(executable) +
    required("index") +
    optional("-t", nCoresRequest) +
    required(input) +
    required(output)
}

object SambambaIndex {
  def apply(root: Configurable, input: File, output: File): SambambaIndex = {
    val indexer = new SambambaIndex(root)
    indexer.input = input
    indexer.output = output
    return indexer
  }

  def apply(root: Configurable, input: File, outputDir: String): SambambaIndex = {
    val dir = if (outputDir.endsWith("/")) outputDir else outputDir + "/"
    val outputFile = new File(dir + swapExtension(input.getName))
    return apply(root, input, outputFile)
  }

  def apply(root: Configurable, input: File): SambambaIndex = {
    return apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".bam.bai"
}