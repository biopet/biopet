package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
 * Created by ahbbollen on 24-9-15.
 */
abstract class ManweSamplesActivate extends Manwe {

  @Output(doc = "output file")
  var output: File = _

  @Argument(doc = "uri to sample to activate")
  var uri: Option[String] = _

  def subCommand = {
    required("samples") + required("activate") +
    required(uri) + " > " + required(output)
  }


}
