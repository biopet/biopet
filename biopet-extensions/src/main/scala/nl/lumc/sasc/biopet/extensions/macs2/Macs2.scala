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
package nl.lumc.sasc.biopet.extensions.macs2

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}

import scala.util.matching.Regex

/**
  * General igvtools extension
  *
  * Created by sajvanderzeeuw on 12/19/14.
  */
abstract class Macs2 extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "macs2", namespace = "macs2", freeVar = false)
  def versionCommand: String = executable + " --version"
  def versionRegex: Regex = """macs2 (.*)""".r
  override def versionExitcode = List(0, 1)
}
