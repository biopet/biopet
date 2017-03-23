package nl.lumc.sasc.biopet.extensions.wisecondor

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by Sander Bollen on 20-3-17.
 */
class WisecondorGcCorrect(val root: Configurable) extends Wisecondor {

  @Input
  var inputBed: File = _

  var fracN: Option[Float] = config("frac_n", namespace = "wisecondor", default = None)
  var fracR: Option[Float] = config("frac_r", namespace = "wisecondor", default = None)
  var nIter: Option[Int] = config("iter", namespace = "wisecondor", default = None)
  var fracLowess: Option[Float] = config("frac_lowess", namespace = "wisecondor", default = None)

  def cmdLine = executable +
    required("gc-correct") +
    required("-I", inputBed) +
    required("-R", referenceFasta()) +
    required("-O", output) +
    binCommand +
    optional("-n", fracN) +
    optional("-r", fracR) +
    optional("-t", nIter) +
    optional("-l", fracLowess)
}
