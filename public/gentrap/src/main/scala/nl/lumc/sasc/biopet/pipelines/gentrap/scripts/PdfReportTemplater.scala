/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.pipelines.gentrap.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Wrapper for the pdf_report.py script, used internally in Gentrap
 */
class PdfReportTemplater(val root: Configurable) extends PythonCommandLineFunction {

  setPythonScript("pdf_report.py")

  @Input(doc = "Input summary file", required = true)
  var summaryFile: File = null

  @Input(doc = "Main report template", required = true)
  var mainTemplateFile: File = null

  @Output(doc = "Output file", required = true)
  var output: File = null

  def cmdLine = {
    getPythonCommand +
      required(summaryFile) +
      required(mainTemplateFile) +
      " > " + required(output)
  }
}
