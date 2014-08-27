/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.extensions

import java.io.FileOutputStream
import java.io.File
import org.broadinstitute.gatk.utils.commandline.{ Input }
import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import scala.collection.JavaConversions._

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
