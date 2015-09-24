package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
 * Created by ahbbollen on 24-9-15.
 */
abstract class ManweDataSourcesList extends Manwe {

  @Argument(doc = "User uri to filter by")
  var user: Option[String] = _

  def subCommand = {
    required("data-sources") + required("list") +
    optional("-u", user)
  }


}


