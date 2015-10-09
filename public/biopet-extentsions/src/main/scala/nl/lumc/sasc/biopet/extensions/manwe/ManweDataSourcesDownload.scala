package nl.lumc.sasc.biopet.extensions.manwe

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Argument

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweDataSourcesDownload(val root: Configurable) extends Manwe {

  @Argument(doc = "uri to data source to download")
  var uri: String = _

  def subCommand = {
    required("data-sources") + required("download") + required(uri)
  }

  this.deps

}
