package nl.lumc.sasc.biopet.scripts

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File

class PrefixFastq(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("prefixFastq.py")

  @Input(doc = "Input file")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  @Argument(doc = "Prefix sequence")
  var prefix: String = "CATG"

  @Argument(doc = "Input file is gziped", required = false)
  var gzip: Boolean = _

  override def beforeCmd {
    if (input.getName.endsWith(".gzip") || input.getName.endsWith("gz")) gzip = true
  }

  def cmdLine = getPythonCommand +
    required("-o", output) +
    required("--prefix", prefix) +
    required(input)
}

object PrefixFastq {
  def apply(root: Configurable, input: File, outputDir: String): PrefixFastq = {
    val prefixFastq = new PrefixFastq(root)
    prefixFastq.input = input
    prefixFastq.output = new File(outputDir, input.getName + ".prefix.fastq")
    return prefixFastq
  }
}
