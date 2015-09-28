package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output }

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweSamplesImportBed(val root: Configurable) extends Manwe {

  /**
   * Import bed for existing sample
   */

  @Argument(doc = "uri of sample to upload to")
  var uri: Option[String] = _

  @Argument(doc = "path to VCF file to upload")
  var bed: File = _

  @Argument(doc = "flag if data is already uploaded?") // TODO: What is the use of this flag even? We're specifically uploading with this command
  var alreadyUploaded: Boolean = false

  def subCommand = {
    required("samples") + required("import-bed") +
      required(uri) + required(bed) +
      conditional(alreadyUploaded, "-u")
  }

}
