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

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Output

/**
 * Created by pjvan_thof on 8/11/15.
 */
class Curl(val root: Configurable) extends BiopetCommandLineFunction with Version {
  @Output
  var output: File = _

  var url: String = _

  executable = config("exe", default = "curl")
  def versionCommand = executable + " --version"
  def versionRegex = """curl (\w+\.\w+\.\w+) .*""".r

  def cmdLine: String = required(executable) + required(url) + (if (outputAsStsout) "" else " > " + required(output))
}
