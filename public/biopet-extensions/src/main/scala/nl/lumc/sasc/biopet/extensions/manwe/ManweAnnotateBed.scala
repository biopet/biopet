package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweAnnotateBed(val root: Configurable) extends Manwe {

  @Input(doc = "the bed to annotate")
  var bed: File = _

  @Argument(doc = "flag if data has already been uploaded")
  var alreadyUploaded: Boolean = false

  @Argument(doc = "Flag whether to wait for annotation to complete on the server")
  var waitToComplete: Boolean = false

  @Argument(doc = "annotation queries", required = false)
  var queries: List[String] = Nil

  def subCommand = {
    required("annotate-bed") + required(bed) +
      conditional(alreadyUploaded, "-u") +
      repeat("-q", queries) +
      conditional(waitToComplete, "--wait")
  }

}
