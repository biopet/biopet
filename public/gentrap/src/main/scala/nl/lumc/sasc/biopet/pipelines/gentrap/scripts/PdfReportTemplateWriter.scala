/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.pipelines.gentrap.scripts

import java.io.File

import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction

/**
 * Wrapper for the pdf_report.py script, used internally in Gentrap
 */
class PdfReportTemplater(val root: Configurable) extends PythonCommandLineFunction {

  val templateResDir: File = new File(getClass.getResource("/nl/lumc/sasc/biopet/pipelines/gentrap/templates/pdf").toURI)
  val templateWorkDir: File = new File(".queue/tmp/nl/lumc/sasc/biopet/pipelines/gentrap/templates/pdf")

  @Input(doc = "Input summary file", required = true)
  var summaryFile: File = null

  @Input(doc = "Main report template", required = true)
  var mainTemplateFile: File = null

  @Output(doc = "Output file", required = true)
  var output: File = null

  protected def prepTemplate(): Unit = {
    FileUtils.copyDirectory(templateResDir, templateWorkDir)
  }

  def cmdLine = {
    getPythonCommand +
      required(summaryFile) +
      required(new File(templateWorkDir, "main.tex")) +
      " > " + required(output)
  }

  setPythonScript("pdf_report.py")
  prepTemplate()
}
