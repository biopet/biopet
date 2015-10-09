package nl.lumc.sasc.biopet.extensions.manwe

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Argument

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweDataSourcesAnnotate(val root: Configurable) extends Manwe {

  @Argument(doc = "uri to data source to annotate")
  var uri: Option[String] = _

  @Argument(doc = "list of queries")
  var queries: List[String] = Nil

  @Argument(doc = "Flag whether to wait for annotation to complete on server")
  var waitToComplete: Boolean = false

  def subCommand = {
    required("data-sources") + required("annotate") +
      required(uri) + repeat("-q", queries) +
      conditional(waitToComplete, "--wait")
  }

}
