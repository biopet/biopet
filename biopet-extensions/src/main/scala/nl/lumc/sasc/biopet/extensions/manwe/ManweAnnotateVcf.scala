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
import org.broadinstitute.gatk.utils.commandline.{Argument, Input}

/**
  * Created by ahbbollen on 24-9-15.
  */
class ManweAnnotateVcf(val parent: Configurable) extends Manwe {

  @Input(doc = "the vcf to annotate")
  var vcf: File = _

  @Argument(doc = "flag if data has already been uploaded")
  var alreadyUploaded: Boolean = false

  @Argument(doc = "flag whether to wait for annotation to complete")
  var waitToComplete: Boolean = false

  @Argument(doc = "annotation queries", required = false)
  var queries: List[String] = Nil

  def subCommand: String = {
    required("annotate-vcf") + required(vcf) +
      conditional(alreadyUploaded, "-u") +
      repeat("-q", queries) +
      conditional(waitToComplete, "--wait")
  }

}
