/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions.manwe

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Argument

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweDataSourcesAnnotate(val root: Configurable) extends Manwe {

  @Argument(doc = "uri to data source to annotate")
  var uri: Option[String] = _

  @Argument(doc = "list of queries", required = false)
  var queries: List[String] = Nil

  @Argument(doc = "Flag whether to wait for annotation to complete on server")
  var waitToComplete: Boolean = false

  def subCommand = {
    required("data-sources") + required("annotate") +
      required(uri) +
      repeat("-q", queries) +
      conditional(waitToComplete, "--wait")
  }

}
