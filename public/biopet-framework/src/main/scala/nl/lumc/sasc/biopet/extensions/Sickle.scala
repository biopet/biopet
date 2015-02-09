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

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class Sickle(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "R1 input")
  var input_R1: File = _

  @Input(doc = "R2 input", required = false)
  var input_R2: File = _

  @Output(doc = "R1 output")
  var output_R1: File = _

  @Output(doc = "R2 output", required = false)
  var output_R2: File = _

  @Output(doc = "singles output", required = false)
  var output_singles: File = _

  @Output(doc = "stats output")
  var output_stats: File = _

  var fastqc: Fastqc = _

  executable = config("exe", default = "sickle", freeVar = false)
  var qualityType: Option[String] = config("qualitytype")
  var qualityThreshold: Option[Int] = config("qualityThreshold")
  var lengthThreshold: Option[Int] = config("lengthThreshold")
  var noFiveprime: Boolean = config("noFiveprime", default = false)
  var discardN: Boolean = config("discardN", default = false)
  var quiet: Boolean = config("quiet", default = false)
  var defaultQualityType: String = config("defaultqualitytype", default = "sanger")
  override val versionRegex = """sickle version (.*)""".r
  override def versionCommand = executable + " --version"

  override def afterGraph {
    if (qualityType.isEmpty) qualityType = Some(defaultQualityType)
  }

  def cmdLine = {
    var cmd: String = required(executable)
    if (input_R2 != null) {
      cmd += required("pe") +
        required("-r", input_R2) +
        required("-p", output_R2) +
        required("-s", output_singles)
    } else cmd += required("se")
    cmd +
      required("-f", input_R1) +
      required("-t", qualityType) +
      required("-o", output_R1) +
      optional("-q", qualityThreshold) +
      optional("-l", lengthThreshold) +
      optional("-x", noFiveprime) +
      optional("-n", discardN) +
      optional("--quiet", quiet) +
      " > " + required(output_stats)
  }
}
