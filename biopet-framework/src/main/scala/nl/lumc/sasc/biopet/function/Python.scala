package nl.lumc.sasc.biopet.function

import java.io.FileOutputStream
import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.collection.JavaConversions._

trait Python extends CommandLineFunction {
  @Argument(doc="Pyhton exe", shortName="script")
  var python_exe: String = "python"
  @Input(doc="Pyhton script", shortName="script", required=false)
  var python_script: File = _
    
  private var python_script_name : String = _
  def setPythonScript(script:String, subpackage:String) {
    python_script_name = script
    //val pack = getClass.getPackage.toString.replaceAll(".", "/")
    //logger.info(pack)
//    for (r <- getClass.getClassLoader.getResources("/nl/lumc/sasc/biopet/pipelines/flexiprep/scripts/*")) {
//      logger.info(r)
//      logger.info(r.getContent)
//    }
    python_script = new File(".queue/tmp/" + subpackage + python_script_name)
    if (!python_script.getParentFile.exists) python_script.getParentFile.mkdirs
    val is = getClass.getResourceAsStream(subpackage + python_script_name)
    val os = new FileOutputStream(python_script)
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }
    
  def getPythonCommand() : String = {
    required(python_exe) + required(python_script)
  }
}
