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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input}

import scala.util.matching.Regex

/**
  * Created by pjvan_thof on 8/11/15.
  */
class TarExtract(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  @Input(required = true)
  var inputTar: File = _

  @Argument(required = true)
  var outputDir: File = _

  executable = config("exe", default = "tar", freeVar = false)
  def versionCommand: String = executable + " --version"
  def versionRegex: Regex = """tar \(GNU tar\) (.*)""".r

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    jobLocalDir = outputDir
    jobOutputFile = new File(outputDir, "." + inputTar.getName + ".tar.out")
  }

  def cmdLine: String =
    required(executable) +
      required("-x") +
      required("-f", inputTar) +
      required("--directory", outputDir)
}
