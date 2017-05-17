package nl.lumc.sasc.biopet.pipelines.tarmac.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.extensions.PythonCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by Sander Bollen on 11-5-17.
  */
class TarmacPlot(val parent: Configurable) extends PythonCommandLineFunction {
  setPythonScript("tarmac_plot.py")

  @Input
  var callFile: File = _

  @Input
  var wisecondorFile: File = _

  @Input
  var xhmmFile: File = _

  @Input
  var stouffFile: File = _

  var margin: Int = config("plot_margin", namespace = "tarmac", default = 5000)

  @Output
  var outputDir: File = _

  def cmdLine: String = {
    getPythonCommand +
      required("-c", callFile) +
      required("-w", wisecondorFile) +
      required("-s", stouffFile) +
      required("-x", xhmmFile) +
      required("-m", margin) +
      required("-o", outputDir)
  }

}
