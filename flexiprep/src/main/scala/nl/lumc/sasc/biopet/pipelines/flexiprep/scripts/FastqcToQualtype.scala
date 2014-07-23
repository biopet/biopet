package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import nl.lumc.sasc.biopet.core.config.Configurable

import nl.lumc.sasc.biopet.function.PythonCommandLineFunction

class FastqcToQualtype(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("__init__.py", "pyfastqc/")
  setPythonScript("qual_type_sickle.py")

  @Input(doc = "Fastqc output", shortName = "fastqc", required = true)
  var fastqc_output: File = _

  @Output(doc = "Output file", shortName = "out", required = true)
  var out: File = _

  def cmdLine = {
    getPythonCommand +
      required(fastqc_output.getParent()) +
      " > " +
      required(out)
  }
}
