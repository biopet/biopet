package nl.lumc.sasc.biopet.extensions.manwe

import org.broadinstitute.gatk.utils.commandline.Argument

/**
 * Created by ahbbollen on 24-9-15.
 */
abstract class ManweDataSourcesAnnotate extends Manwe {

  @Argument(doc = "uri to data source to annotate")
  var uri: Option[String] = _

  @Argument(doc = "list of queries")
  var queries: List[String] = Nil

  def subCommand = {
    required("data-sources") + required("annotate") +
    required(uri) + repeat("-q", queries)
  }



}
