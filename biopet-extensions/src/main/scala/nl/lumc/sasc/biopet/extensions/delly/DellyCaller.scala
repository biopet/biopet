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
package nl.lumc.sasc.biopet.extensions.delly

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

class DellyCaller(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "delly")

  private lazy val versionexecutable: File = new File(executable)

  override def defaultThreads = 1
  override def defaultCoreMemory = 4.0

  def versionCommand = versionexecutable.getAbsolutePath
  def versionRegex = """DELLY \(Version: (.*)\)""".r
  override def versionExitcode = List(0, 1)

  @Input(doc = "Input file (bam)")
  var input: File = _

  @Output(doc = "Delly VCF output")
  var outputvcf: File = _

  @Argument(doc = "What kind of analysis to run: DEL,DUP,INV,TRA")
  var analysistype: String = _

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    deps ::= new File(input.getParentFile, input.getName + ".bai")
  }

  def cmdLine = required(executable) +
    "-t" + required(analysistype) +
    "-o" + required(outputvcf) +
    required(input)

}
