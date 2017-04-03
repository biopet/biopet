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

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Version }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.util.matching.Regex

/**
 * Created by pjvanthof on 18/05/16.
 */
class Sed(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "sed", freeVar = false)

  /** Command to get version of executable */
  override def versionCommand: String = executable + " --version"

  /** Regex to get version from version command output */
  override def versionRegex: Regex = """sed (GNU sed) \d+.\d+.\d+""".r

  @Input(required = false)
  var inputFile: File = _

  @Output
  var outputFile: File = _

  var expressions: List[String] = Nil

  def cmdLine = executable +
    repeat("-e", expressions) +
    (if (inputAsStdin) "" else required(inputFile)) +
    (if (outputAsStsout) "" else " > " + required(outputFile))

}
