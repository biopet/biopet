package nl.lumc.sasc.biopet.extensions.xhmm

import java.io.File

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * Created by Sander Bollen on 23-11-16.
  */
class XhmmGenotype(val root: Configurable) extends Xhmm with Reference {

  @Input
  var inputMatrix: File = _

  @Input
  var inputXcnv: File = _

  @Output
  var outputVcf: File = _

  @Input
  var r: File = _

  @Argument
  var discoverParamsFile: File =  config("discover_params", namespace = "xhmm")

  def cmdLine = {
    executable + required("--genotype") +
      required("-p", discoverParamsFile) +
      required("-r", inputMatrix) +
      required("-R", r) +
      required("-g", inputXcnv) +
      required("-F", referenceFasta()) +
      required("-v", outputVcf)
  }



}
