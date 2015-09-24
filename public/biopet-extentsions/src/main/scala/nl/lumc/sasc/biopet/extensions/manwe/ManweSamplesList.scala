package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
 * Created by ahbbollen on 23-9-15.
 */
abstract class ManweSamplesList extends Manwe {

  @Argument(doc = "filter by user URI")
  var user: Option[String] = None

  @Argument(doc = "filter by group URI")
  var group: List[String] = Nil

  var onlyPublic: Boolean = false

  def subCommand = {
    required("samples") + required("list") + optional("-u", user) +
    repeat("-g", group) + conditional(onlyPublic, "-p")
  }






}
