package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
 * Created by ahbbollen on 24-9-15.
 */
abstract class ManweDataSourcesShow extends Manwe {

  @Argument(doc = "uri of data source")
  var uri:  Option[String] = _

  def subCommand = {
    required("data-sources") + required("show") +
    required(uri)
  }

}
