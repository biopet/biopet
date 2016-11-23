package nl.lumc.sasc.biopet.extensions.xhmm

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * Created by Sander Bollen on 23-11-16.
  */
class XhmmDiscover(val root: Configurable) extends Xhmm {

  @Input
  var inputMatrix: File = _

  @Output
  var outputXcnv: File = _

  @Input
  var r: File = _

  @Argument
  var discoverParamsFile: File = config("discover_params", namespace = "xhmm")

  private lazy val outputXcnvAuxFile: File = {
    new File(outputXcnv.getAbsolutePath + ".aux")
  }

  @Argument
  var xhmmAnalysisName: String = _

  def cmdLine = {
    executable + required("--discover") +
      required("-r", inputMatrix) +
      required("-R", r) +
      required("-c", outputXcnv) +
      required("-a", outputXcnvAuxFile) +
      required("-s", xhmmAnalysisName)
  }

}
