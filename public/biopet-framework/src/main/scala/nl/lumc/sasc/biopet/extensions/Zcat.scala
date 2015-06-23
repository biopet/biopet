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
package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

/** Extension for zcat */
class Zcat(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Zipped file")
  var input: File = _

  @Output(doc = "Unzipped file")
  var output: File = _

  executable = config("exe", default = "zcat")

  override val versionRegex = """zcat \(gzip\) (.*)""".r
  override def versionCommand = executable + " --version"

  /** Returns command to execute */
  def cmdLine = required(executable) + required(input) + " > " + required(output)
}

object Zcat {
  /** Returns a default zcat */
  def apply(root: Configurable, input: File, output: File): Zcat = {
    val zcat = new Zcat(root)
    zcat.input = input
    zcat.output = output
    return zcat
  }
}