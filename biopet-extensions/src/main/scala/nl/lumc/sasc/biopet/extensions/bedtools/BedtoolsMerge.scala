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

  @Argument(doc = "Distance", required = false)
  var dist: Option[Int] = config("dist", default = 1) //default of tool is 1

  @Output(doc = "Output bed file")
  var output: File = _

  @Argument(doc = "operation to additional columns", required = false)
  var operation: Option[String] = None

  @Argument(doc = "Additional columns to operate upon", required = false)
  var additionalColumns: List[Int] = Nil

  def cmdLine: String = {
    required(executable) + required("merge") +
      required("-i", input) + optional("-d", dist) +
      (if (additionalColumns.nonEmpty) required("-c", additionalColumns.mkString(",")) else "") +
      optional("-o", operation) +
      " > " + required(output)
  }

}
