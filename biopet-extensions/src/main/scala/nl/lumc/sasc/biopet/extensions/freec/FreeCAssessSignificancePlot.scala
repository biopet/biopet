package nl.lumc.sasc.biopet.extensions.freec

import java.io.File

import nl.lumc.sasc.biopet.core.extensions.RscriptCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class FreeCAssessSignificancePlot(val root: Configurable) extends RscriptCommandLineFunction {
  protected var script: File = new File("/nl/lumc/sasc/biopet/extensions/freec/freec_assess_significance.R")

  @Input(doc = "Output file from FreeC. *_CNV", required = true)
  var cnv: File = null

  @Input(doc = "Output file from FreeC. *_ratio.txt", required = true)
  var ratios: File = null

  @Output(doc = "Destination for the PNG file", required = true)
  var output: File = null

  /* cmdLine to execute R-script and with arguments
   * Arguments should be pasted in the same order as the script is expecting it.
   * Unless some R library is used for named arguments
   * */
  override def cmdLine: String = {
    super.cmdLine +
      required("--cnv", cnv) +
      required("--ratios", ratios) +
      required("--output", output)

  }
}
