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

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
  * Created by ahbbollen on 23-9-15.
  */
class ManweSamplesList(val parent: Configurable) extends Manwe {

  @Argument(doc = "filter by user URI", required = false)
  var user: Option[String] = None

  @Argument(doc = "filter by group URI", required = false)
  var group: List[String] = Nil

  var onlyPublic: Boolean = false

  def subCommand: String = {
    required("samples") +
      required("list") +
      optional("-u", user) +
      repeat("-g", group) +
      conditional(onlyPublic, "-p")
  }

}
