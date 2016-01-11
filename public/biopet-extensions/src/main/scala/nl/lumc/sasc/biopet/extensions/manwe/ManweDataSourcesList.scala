package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output }

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweDataSourcesList(val root: Configurable) extends Manwe {

  @Argument(doc = "User uri to filter by")
  var user: Option[String] = _

  def subCommand = {
    required("data-sources") +
      required("list") +
      optional("-u", user)
  }

}

