package nl.lumc.sasc.biopet.pipelines.tarmac.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.extensions.PythonCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Created by Sander Bollen on 21-4-17.
 */
class SampleFromMatrix(val parent: Configurable) extends PythonCommandLineFunction {
  setPythonScript("select_sample_from_matrix.py")

  @Input
  var inputMatrix: File = _

  @Argument
  var sample: String = _

  @Output(required = false)
  var output: Option[File] = None

  def cmdLine: String = {
    getPythonCommand +
      required("-I", inputMatrix) +
      required("-s", sample) +
      (if (outputAsStsout) "" else " > " + required(output))
  }
}
