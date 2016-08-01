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
package nl.lumc.sasc.biopet.extensions.kraken

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/** Extension for Kraken */
class KrakenReport(val root: Configurable) extends BiopetCommandLineFunction with Version {

  executable = config("exe", default = "kraken-report")
  def versionRegex = """Kraken version (.*)""".r
  override def versionExitcode = List(0, 1)

  override def defaultCoreMemory = 4.0
  override def defaultThreads = 1

  def versionCommand = {
    val exe = new File(new File(executable).getParent, "kraken")
    if (exe.exists()) exe.getAbsolutePath + " --version"
    else executable + " --version"
  }

  var db: File = config("db")
  var showZeros: Boolean = config("show_zeros", default = false)

  @Input(doc = "Input raw kraken analysis")
  var input: File = _

  @Output(doc = "Output path kraken report")
  var output: File = _

  def cmdLine: String = required(executable) +
    required("--db", db) +
    conditional(showZeros, "--show-zeros") +
    required(input) +
    " > " + required(output)
}
