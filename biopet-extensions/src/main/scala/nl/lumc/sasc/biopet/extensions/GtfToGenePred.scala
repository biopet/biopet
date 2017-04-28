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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by pjvan_thof on 17-5-16.
  */
class GtfToGenePred(val parent: Configurable) extends BiopetCommandLineFunction {
  executable = config("exe", default = "gtfToGenePred", freeVar = false)

  @Input
  var inputGtfs: List[File] = Nil

  @Output
  var outputGenePred: File = _

  @Output
  var infoOut: Option[File] = None

  var genePredExt: Boolean = config("gene _pred _ext", default = false)
  var allErrors: Boolean = config("all_errors", default = false)
  var impliedStopAfterCds: Boolean = config("implied_stop_after_cds", default = false)
  var simple: Boolean = config("simple", default = false)
  var geneNameAsName2: Boolean = config("gene _name_as_name2", default = false)

  def cmdLine =
    executable +
      conditional(genePredExt, "-genePredExt") +
      conditional(allErrors, "-allErrors") +
      optional("-infoOut", infoOut) +
      conditional(allErrors, "-allErrors") +
      conditional(impliedStopAfterCds, "-impliedStopAfterCds") +
      conditional(simple, "-simple") +
      conditional(geneNameAsName2, "-geneNameAsName2") +
      repeat(inputGtfs) +
      (if (outputAsStsout) required("/dev/stdout") else required(outputGenePred))
}
