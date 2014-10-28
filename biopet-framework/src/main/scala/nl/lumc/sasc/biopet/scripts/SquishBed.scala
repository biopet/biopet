package nl.lumc.sasc.biopet.scripts

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class SquishBed(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("bed_squish.py")

  @Input(doc = "Input file")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  def cmdLine = getPythonCommand +
    required(input) +
    required(output)
}

object SquishBed {
  def apply(root: Configurable, input: File, outputDir: String): SquishBed = {
    val squishBed = new SquishBed(root)
    squishBed.input = input
    squishBed.output = new File(outputDir, input.getName.stripSuffix(".bed") + ".squish.bed")
    return squishBed
  }
}
