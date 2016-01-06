package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output }

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweSamplesImportVcf(val root: Configurable) extends Manwe {

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
