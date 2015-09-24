package nl.lumc.sasc.biopet.extensions.manwe

import org.broadinstitute.gatk.utils.commandline.Argument

/**
 * Created by ahbbollen on 24-9-15.
 */
abstract class ManweDataSourcesDownload extends Manwe {

  @Argument(doc = "uri to data source to download")
  var uri: Option[String] = _

  def subCommand = {
    required("data-sources") + required("download") + required(uri)
  }

}
