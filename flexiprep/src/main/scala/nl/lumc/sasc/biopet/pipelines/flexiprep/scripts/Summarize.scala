package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.function.PythonCommandLineFunction

class Summarize(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("__init__.py", "pyfastqc/")
  setPythonScript("summarize_flexiprep.py")

  @Output(doc = "Output file", shortName = "out", required = true)
  var out: File = _

  var samplea: String = _
  var sampleb: String = _
  var runDir: String = _
  var samplename: String = _
  var trim: Boolean = true
  var clip: Boolean = true

  def cmdLine = {
    var mode: String = ""
    if (clip) mode += "clip"
    if (trim) mode += "trim"
    if (mode.isEmpty) mode = "none"

    getPythonCommand +
      optional("--run-dir", runDir) +
      optional("--sampleb", sampleb) +
      required(samplename) +
      required(mode) +
      required(samplea) +
      required(out)
  }
}
