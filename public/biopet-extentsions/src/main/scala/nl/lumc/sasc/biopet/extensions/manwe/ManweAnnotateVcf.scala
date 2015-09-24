package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input}

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweAnnotateVcf(val root: Configurable) extends Manwe {

  @Input(doc = "the vcf to annotate")
  var vcf: File = _

  @Argument(doc = "flag if data has already been uploaded")
  var alreadyUploaded: Boolean = false

  @Argument(doc = "annotation queries")
  var queries: List[String] = Nil

  def subCommand = {
    required("annotate-vcf") + required(vcf) +
    conditional(alreadyUploaded, "-u") +
    repeat("-q", queries)
  }




}
