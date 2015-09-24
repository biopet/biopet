package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
 * Created by ahbbollen on 24-9-15.
 */
abstract class ManweSamplesAnnotateVariations extends Manwe {


  @Argument(doc= "uri to sample to annotate")
  var uri: Option[String] = _

  @Argument(doc = "Annotation queries")
  var queries: List[String] = Nil

  def subCommand = {
    required("samples") + required("annotate-variations") +
    required("uri") + repeat("-q", queries)
  }




}
