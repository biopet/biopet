package nl.lumc.sasc.biopet.function

import java.io.FileOutputStream
import java.io.File
import org.broadinstitute.gatk.utils.commandline.{ Input }
import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import scala.collection.JavaConversions._

trait PythonCommandLineFunction extends BiopetCommandLineFunction {
  @Input(doc = "Python script", required = false)
  var python_script: File = _

  executable = config("exe", default = "python", submodule = "python")

  protected var python_script_name: String = _
  def setPythonScript(script: String) { setPythonScript(script, "") }
  def setPythonScript(script: String, subpackage: String) {
    python_script_name = script
    python_script = new File(".queue/tmp/" + subpackage + python_script_name)
    if (!python_script.getParentFile.exists) python_script.getParentFile.mkdirs
    val is = getClass.getResourceAsStream(subpackage + python_script_name)
    val os = new FileOutputStream(python_script)
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  def getPythonCommand(): String = {
    required(executable) + required(python_script)
  }
}
