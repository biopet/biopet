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
package nl.lumc.sasc.biopet.extensions.sambamba

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class SambambaMpileup(val root: Configurable) extends Sambamba {
  override val defaultThreads = 4

  @Input(doc = "Bam File")
  var input: List[File] = Nil

  @Output(doc = "Output file", required = false)
  var output: File = null

  val buffer: Option[Int] = config("buffer", default = 8 * 1024 * 1024)

  def cmdLine = {
    required(executable) +
      required("mpileup") +
      optional("-t", threads) +
      optional("-b", buffer) +
      repeat(input) + " | " +
      "pigz -9 -p " + threads + " -i -c > " +
      output.getAbsolutePath
  }
}
