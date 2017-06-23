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
package nl.lumc.sasc.biopet.extensions.vt

import java.io.File

import nl.lumc.sasc.biopet.core.{Reference, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by pjvanthof on 20/11/15.
  */
class VtDecompose(val parent: Configurable) extends Vt with Version with Reference {
  def versionRegex = """decompose (.*)""".r
  override def versionExitcode = List(0, 1)
  def versionCommand = executable + " decompose"

  @Input(required = true)
  var inputVcf: File = _

  @Output(required = true)
  var outputVcf: File = _

  var intervalsFile: Option[File] = config("intervals_file")

  val smartDecompose: Boolean = config("smart_decompose", default = false)

  def cmdLine =
    required(executable) + required("decompose") +
      required("-o", outputVcf) +
      optional("-I", intervalsFile) +
      conditional(smartDecompose, "-s") +
      required(inputVcf)
}
