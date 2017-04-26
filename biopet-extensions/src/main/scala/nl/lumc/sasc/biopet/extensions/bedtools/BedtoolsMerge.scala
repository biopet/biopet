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
package nl.lumc.sasc.biopet.extensions.bedtools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Output, Input}

/**
  * Created by ahbbollen on 5-1-16.
  */
class BedtoolsMerge(val parent: Configurable) extends Bedtools {

  @Input(doc = "Input bed file")
  var input: File = _

  @Argument(doc = "Distance")
  var dist: Option[Int] = config("dist") //default of tool is 1

  @Output(doc = "Output bed file")
  var output: File = _

  def cmdLine = {
    required(executable) + required("merge") +
      required("-i", input) + optional("-d", dist) +
      " > " + required(output)
  }

}
