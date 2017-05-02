package nl.lumc.sasc.biopet.pipelines.tarmac.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.extensions.PythonCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Created by Sander Bollen on 24-4-17.
 */
class BedThreshold(val parent: Configurable) extends PythonCommandLineFunction {
  setPythonScript("bed_threshold.py")

  @Input
  var input: File = _

  @Argument
  var threshold: Int = _

  @Output(required = false)
  var output: Option[File] = None

  def cmdLine: String = {
    getPythonCommand +
      required("-i", input) +
      required("-t", threshold) +
      (if (outputAsStsout) "" else " > " + required(output))
  }
}
