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
package nl.lumc.sasc.biopet.extensions.bowtie

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Version }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Created by pjvan_thof on 8/15/15.
 */
class Bowtie2Build(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  @Input(required = true)
  var reference: File = _

  @Argument(required = true)
  var baseName: String = _

  executable = config("exe", default = "bowtie2-build", freeVar = false)
  def versionRegex = """.*[Vv]ersion:? (\d*\.\d*\.\d*)""".r
  def versionCommand = executable + " --version"

  override def defaultCoreMemory = 15.0

  @Output
  private var outputFiles: List[File] = Nil

  override def beforeGraph: Unit = {
    outputFiles ::= new File(reference.getParentFile, baseName + ".1.bt2")
    outputFiles ::= new File(reference.getParentFile, baseName + ".2.bt2")
  }

  def cmdLine = required("cd", reference.getParentFile) + " && " +
    required(executable) +
    required(reference) +
    required(baseName)
}
