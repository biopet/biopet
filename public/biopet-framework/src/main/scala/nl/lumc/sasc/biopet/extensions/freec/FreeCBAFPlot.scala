package nl.lumc.sasc.biopet.extensions.freec

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.RscriptCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class FreeCBAFPlot(val root: Configurable) extends RscriptCommandLineFunction {
  protected var script: File = new File("/nl/lumc/sasc/biopet/extensions/freec/freec_BAFPlot.R")

  @Input(doc = "Output file from FreeC. *_BAF.txt")
  var input: File = null

  @Output(doc = "Destination for the PNG file")
  var output: File = null

  /* cmdLine to execute R-script and with arguments
   * Arguments should be pasted in the same order as the script is expecting it.
   * Unless some R library is used for named arguments
   * */
  override def cmdLine: String = {
    super.cmdLine +
      required("-i", input.getAbsolutePath) +
      required("-o", output.getAbsolutePath)
  }

}
