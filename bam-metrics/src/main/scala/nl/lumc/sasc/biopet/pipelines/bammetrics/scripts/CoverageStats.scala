package nl.lumc.sasc.biopet.pipelines.bammetrics.scripts

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class CoverageStats(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("bedtools_cov_stats.py")

  @Input(doc = "Input file")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  @Output(doc = "plot File (png)")
  var plot: File = _
  
  override val defaultVmem = "12G"

  def cmdLine = getPythonCommand +
    required(input) + required("--plot", plot) + " > " + required(output)
}

object CoverageStats {
  def apply(root: Configurable, input: File, outputDir: String): CoverageStats = {
    val coverageStats = new CoverageStats(root)
    coverageStats.input = input
    coverageStats.output = new File(outputDir, input.getName + ".stats")
    coverageStats.plot = new File(outputDir, input.getName + ".stats.png")
    return coverageStats
  }
}
