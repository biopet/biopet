package nl.lumc.sasc.biopet.extensions.xhmm

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Created by Sander Bollen on 23-11-16.
 */
class XhmmNormalize(val root: Configurable) extends Xhmm {

  @Input
  var inputMatrix: File = _

  @Input
  var pcaFile: File = _

  @Output
  var normalizeOutput: File = _

  var normalizeMethod: String = config("normalize_method", namespace = "xhmm_normalize", default = "PVE_mean")

  var pveFactor: Float = config("pve_mean_factor", namespace = "xhmm_normalize", default = 0.7)

  def cmdLine = {
    executable + required("--normalize") +
      required("-r", inputMatrix) +
      required("--PCAfiles", pcaFile) +
      required("--normalizeOutput", normalizeOutput) +
      required("--PCnormalizeMethod", normalizeMethod) +
      required("--PVE_mean_factor", pveFactor)
  }

}
