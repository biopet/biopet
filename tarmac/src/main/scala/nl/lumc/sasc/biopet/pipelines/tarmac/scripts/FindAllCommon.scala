package nl.lumc.sasc.biopet.pipelines.tarmac.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.extensions.PythonCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by Sander Bollen on 23-6-17.
  */
class FindAllCommon(val parent: Configurable) extends PythonCommandLineFunction {
  setPythonScript("find_all_common.py")

  @Input
  var inputFile: File = _

  @Input
  var databases: List[File] = Nil

  @Output(required = false)
  var output: Option[File] = None

  def cmdLine: String = {
    getPythonCommand +
      required("--input", inputFile) +
      repeat("--db", databases) +
      (if (outputAsStdout) "" else " > " + required(output))
  }

}
