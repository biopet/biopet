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
package nl.lumc.sasc.biopet.core.extensions

import java.io.{ File, FileOutputStream }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.Input

trait PythonCommandLineFunction extends BiopetCommandLineFunction {
  @Input(doc = "Python script", required = false)
  var pythonScript: File = _

  executable = config("exe", default = "python", namespace = "python", freeVar = false)

  protected var pythonScriptName: String = _

  /**
   * checks if script already exist in jar otherwise try to fetch from the jar
   * @param script name / location of script
   */
  def setPythonScript(script: String) {
    pythonScript = new File(script)
    if (!pythonScript.exists()) {
      setPythonScript(script, "")
    } else {
      pythonScriptName = script
    }
  }

  /**
   * Set and extract python script from jar file
   * @param script name of script in jar
   * @param subpackage location of script in jar
   */
  def setPythonScript(script: String, subpackage: String) {
    pythonScriptName = script
    pythonScript = new File(".queue/tmp/" + subpackage + pythonScriptName)
    if (!pythonScript.getParentFile.exists) pythonScript.getParentFile.mkdirs
    val is = getClass.getResourceAsStream(subpackage + pythonScriptName)
    val os = new FileOutputStream(pythonScript)
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  /** return basic command to prefix the complete command with */
  def getPythonCommand: String = {
    required(executable) + required(pythonScript)
  }
}