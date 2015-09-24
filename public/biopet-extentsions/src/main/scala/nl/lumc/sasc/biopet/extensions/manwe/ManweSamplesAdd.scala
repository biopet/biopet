package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
 * Created by ahbbollen on 24-9-15.
 */
abstract class ManweSamplesAdd extends Manwe {


  @Argument(doc = "the sample name")
  var name: Option[String] = _

  @Argument(doc = "the sample groups [uris]")
  var group: List[String] = Nil

  @Argument(doc = "pool size")
  var poolSize: Option[Int] = _

  def subCommand = {
    required("samples") + required("add") + required(name) +
    optional("-s", poolSize) + repeat("-g", group)
  }




}
