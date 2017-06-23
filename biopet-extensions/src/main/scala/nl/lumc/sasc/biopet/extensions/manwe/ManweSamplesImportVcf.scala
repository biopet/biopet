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
  * Created by ahbbollen on 24-9-15.
  */
class ManweSamplesImportVcf(val parent: Configurable) extends Manwe {

  /**
    * Import vcf for existing sample
    */
  @Argument(doc = "uri of sample to upload to")
  var uri: Option[String] = _

  @Argument(doc = "path to VCF file to upload")
  var vcf: File = _

  @Argument(doc = "flag if data is already uploaded?") // TODO: What is the use of this flag even? We're specifically uploading with this command
  var alreadyUploaded: Boolean = false

  @Argument(doc = "Flag when to prefer genotype likelihoods")
  var preferLikelihoods: Boolean = false

  @Argument(doc = " Flag whether to wait for import to complete on server")
  var waitToComplete: Boolean = false

  def subCommand = {
    required("samples") +
      required("import-vcf") +
      required(uri) + required(vcf) +
      conditional(alreadyUploaded, "-u") +
      conditional(preferLikelihoods, "-l") +
      conditional(waitToComplete, "--wait")
  }

}
