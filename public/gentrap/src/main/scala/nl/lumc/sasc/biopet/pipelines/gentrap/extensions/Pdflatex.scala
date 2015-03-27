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
package nl.lumc.sasc.biopet.pipelines.gentrap.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Wrapper for the pdflatex executable
 */
class Pdflatex(val root: Configurable) extends BiopetCommandLineFunction {

  executable = config("exe", default = "pdflatex", freeVar = false)
  override val executableToCanonicalPath = false

  @Input(doc = "Input LaTeX template", required = true)
  var input: File = null

  @Output(doc = "Output directory", required = true)
  var outputDir: File = null

  @Argument(doc = "Job name", required = true)
  var name: String = null

  @Output(doc = "Output PDF file")
  lazy val outputPdf: File = {
    require(name != null && outputDir != null, "Job name and output directory must be defined")
    new File(outputDir, name + ".pdf")
  }

  def cmdLine = {
    // repeating command 3x times to get internal references working correctly
    val singleCommand = required(executable) +
      required("-output-directory", outputDir) +
      required("-jobname", name) +
      required(input)
    Seq(singleCommand, singleCommand, singleCommand).mkString(" && ")
  }
}
