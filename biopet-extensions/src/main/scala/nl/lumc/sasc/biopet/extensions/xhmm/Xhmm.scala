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
package nl.lumc.sasc.biopet.extensions.xhmm

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}

/**
  * Created by Sander Bollen on 23-11-16.
  *
  * Generic abstract class for XHMM commands
  */
abstract class Xhmm extends BiopetCommandLineFunction with Version {

  executable = config("exe", namespace = "xhmm", default = "xhmm")
  var discoverParamsFile: File = config("discover_params", namespace = "xhmm")

  def versionCommand = executable + " --version"
  def versionRegex = """xhmm (.*)""".r
}
