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
package nl.lumc.sasc.biopet.pipelines.gentrap.scripts

import java.io.{ File, FileOutputStream }

import org.apache.commons.io.IOUtils
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction

/**
 * Wrapper for the pdf_report.py script, used internally in Gentrap
 */
class PdfReportTemplateWriter(val root: Configurable) extends PythonCommandLineFunction {

  @Input(doc = "Input summary file", required = true)
  var summaryFile: File = null

  @Input(doc = "Main report template", required = true) // def since we hard-code the template
  def mainTemplateFile: File = new File(templateWorkDir, "main.tex")

  @Input(doc = "Main report logo", required = true) // def since we hard-code the logo
  def logoFile: File = new File(templateWorkDir, "gentrap_front.png")

  @Output(doc = "Output file", required = true)
  var output: File = null

  val templateWorkDir: File = new File(".queue/tmp/nl/lumc/sasc/biopet/pipelines/gentrap/templates/pdf")
  val templateFiles: Seq[String] = Seq(
    "main.tex", "gentrap_front.png",
    "sample.tex", "sample_mapping.tex",
    "lib.tex", "lib_seqeval.tex", "lib_mapping.tex"
  )

  protected def prepTemplate(name: String,
                             subPackage: String = "/nl/lumc/sasc/biopet/pipelines/gentrap/templates/pdf"): Unit = {
    val target = new File(".queue/tmp" + subPackage, name)
    if (!target.getParentFile.exists) target.getParentFile.mkdirs()
    val is = getClass.getResourceAsStream(subPackage + "/" + name)
    val os = new FileOutputStream(target)
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()

    //python_script_name = script
    //python_script = new File(".queue/tmp/" + subpackage + python_script_name)
    //if (!python_script.getParentFile.exists) python_script.getParentFile.mkdirs
    //val is = getClass.getResourceAsStream(subpackage + python_script_name)
    //val os = new FileOutputStream(python_script)
    //org.apache.commons.io.IOUtils.copy(is, os)
    //os.close()
  }

  def cmdLine = {
    getPythonCommand +
      required(summaryFile) +
      required(mainTemplateFile) +
      required(logoFile.getAbsoluteFile) +
      " > " + required(output)
  }

  setPythonScript("pdf_report.py")
  templateFiles.foreach(t => prepTemplate(t))
}
