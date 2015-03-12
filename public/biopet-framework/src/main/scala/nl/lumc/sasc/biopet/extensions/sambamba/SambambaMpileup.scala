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
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class SambambaMpileup(val root: Configurable) extends Sambamba {
  override val defaultThreads = 4

  @Input(doc = "Bam File")
  var input: List[File] = null

  var output: File = null

  val buffer: Option[Int] = config("buffer")

  def cmdLine = required(executable) +
    required("mpileup") +
    optional("-t", threads) +
    optional("-b", buffer) +
    required(input.mkString(" ")) + " | " +
    "pigz -9 -p " + threads + " -i -c > " +
    output.getAbsolutePath
}

object SambambaMpileup {
  def apply(root: Configurable, input: List[File], output: File): SambambaMpileup = {
    val mpileup = new SambambaMpileup(root)
    mpileup.input = input
    mpileup.output = output
    return mpileup
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".bam.bai"
}