package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output }

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweSamplesAdd(val root: Configurable) extends Manwe {

  @Argument(doc = "the sample name")
  var name: Option[String] = _

  @Argument(doc = "the sample groups [uris]", required = false)
  var group: List[String] = Nil

  @Argument(doc = "pool size")
  var poolSize: Option[Int] = _

  def subCommand = {
    required("samples") + required("add") + required(name) +
      optional("-s", poolSize) + repeat("-g", group)
  }

}
