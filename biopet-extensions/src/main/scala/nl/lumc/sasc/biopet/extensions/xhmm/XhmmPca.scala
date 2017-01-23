package nl.lumc.sasc.biopet.extensions.xhmm

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by Sander Bollen on 23-11-16.
 */
class XhmmPca(val root: Configurable) extends Xhmm {

  @Input
  var inputMatrix: File = _

  @Output(required = true)
  var pcaFile: File = _

  def cmdLine = {
    executable + required("--PCA") + required("-r", inputMatrix) + required("--PCAfiles", pcaFile)
  }

}
