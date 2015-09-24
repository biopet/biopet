package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
 * Created by ahbbollen on 24-9-15.
 */
abstract class ManweSamplesShow extends Manwe {

  @Output(doc = "output file")
  var output: File = _

  @Argument(doc = "The sample to show")
  var uri: Option[String] = _

  def subCommand = {
    required("samples") + required("list") + required(uri) +
    " > " + required(output)
  }

}
