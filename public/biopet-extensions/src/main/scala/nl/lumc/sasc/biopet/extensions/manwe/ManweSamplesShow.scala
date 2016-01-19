package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output }

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweSamplesShow(val root: Configurable) extends Manwe {

  @Argument(doc = "The sample to show")
  var uri: Option[String] = _

  def subCommand = {
    required("samples") +
      required("show") +
      required(uri)
  }

}
