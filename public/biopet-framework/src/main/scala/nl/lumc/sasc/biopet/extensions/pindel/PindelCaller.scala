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
package nl.lumc.sasc.biopet.extensions.pindel

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

class PindelCaller(val root: Configurable) extends BiopetCommandLineFunction {
  executable = config("exe", default = "pindel", freeVar = false)

  override def defaultCoreMemory = 5.0
  override def defaultThreads = 8

  override def versionRegex = """Pindel version:? (.*)""".r
  override def versionExitcode = List(1)
  override def versionCommand = executable

  @Input(doc = "The pindel configuration file")
  var input: File = _

  @Input(doc = "Fasta reference")
  var reference: File = config("reference")

  // this is a pointer to where the results files will be stored
  // inside this directory, we can expect files named:
  // <prefix>_D
  // <prefix>_SI
  // <prefix>_I
  // <prefix>_TR
  @Argument(doc = "Work directory")
  var workdir: String = _

  @Output(doc = "Pindel VCF output")
  var output: File = _

  var window_size: Option[Int] = config("window_size", default = 5)

  override def beforeCmd() {
  }

  def cmdLine = required(executable) +
    "-i " + required(input) +
    "-f " + required(reference) +
    "-o " + required(output) +
    optional("-w", window_size) +
    optional("-T", nCoresRequest)
}

object PindelCaller {
  def apply(root: Configurable, input: File, output: File): PindelCaller = {
    val caller = new PindelCaller(root)
    caller.input = input
    caller.output = output
    caller
  }
}
