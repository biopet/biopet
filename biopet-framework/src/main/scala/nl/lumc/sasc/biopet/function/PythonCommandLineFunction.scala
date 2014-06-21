package nl.lumc.sasc.biopet.function

import java.io.FileOutputStream
import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.collection.JavaConversions._

trait PythonCommandLineFunction extends BiopetCommandLineFunction {
  @Input(doc="Python script", required=false)
  var python_script: File = _
  
  executeble = config("python_exe", "python")
  
  protected var python_script_name : String = _
  def setPythonScript(script:String) { setPythonScript(script,"") }
  def setPythonScript(script:String, subpackage:String) {
    python_script_name = script
    python_script = new File(".queue/tmp/" + subpackage + python_script_name)
    if (!python_script.getParentFile.exists) python_script.getParentFile.mkdirs
    val is = getClass.getResourceAsStream(subpackage + python_script_name)
    val os = new FileOutputStream(python_script)
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }
  
  def getPythonCommand() : String = {
    required(executeble) + required(python_script)
  }
}
