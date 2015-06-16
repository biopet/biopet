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

import java.io.{ File, FileOutputStream }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Trait for RScript wrappers
 */
trait RScriptCommandLineFunction extends BiopetCommandLineFunction {

  @Input(doc = "R script file", required = false)
  var RScript: File = _

  executable = config("exe", default = "Rscript", submodule = "rscript")

  protected var RScriptName: String = _

  def setRScript(script: String) {
    setRScript(script, "")
  }

  def setRScript(script: String, subpackage: String) {
    RScriptName = script
    // TODO: set .queue/tmp as a library-wide constant
    RScript = new File(".queue/tmp/" + subpackage + RScriptName)

    if (!RScript.getParentFile.exists)
      RScript.getParentFile.mkdirs

    val is = getClass.getResourceAsStream(subpackage + RScriptName)
    val os = new FileOutputStream(RScript)

    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  def RScriptCommand: String = {
    required(executable) + required(RScript)
  }
}
