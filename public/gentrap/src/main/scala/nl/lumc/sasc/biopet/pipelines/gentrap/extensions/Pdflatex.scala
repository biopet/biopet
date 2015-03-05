/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
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
    Seq(singleCommand, singleCommand, singleCommand).mkString(" '&&' ")
  }
}
