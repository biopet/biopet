package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
 * Created by ahbbollen on 24-9-15.
 */
abstract class ManweSamplesImportVcf extends Manwe {

  /**
   * Import vcf for existing sample
   */

  @Output(doc = "output file")
  var output: File = _

  @Argument(doc = "uri of sample to upload to")
  var uri: Option[String] = _

  @Argument(doc = "path to VCF file to upload")
  var vcf: File = _

  @Argument(doc = "flag if data is already uploaded?")
  // TODO: What is the use of this flag even? We're specifically uploading with this command
  var alreadyUploaded: Boolean = false

  @Argument(doc = "Flag when to prefer genotype likelihoods")
  var preferLikelihoods: Boolean = false

  def subCommand = {
    required("samples") + required("import-vcf") +
    required(uri) + required(vcf) +
    conditional(alreadyUploaded, "-u") + conditional(preferLikelihoods, "-l") +
    " > " + required(output)
  }

}
