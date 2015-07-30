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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions.rscript

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.RscriptCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Extension for en general stackedbar plot with R
 *
 * Created by pjvan_thof on 4/29/15.
 */
class StackedBarPlot(val root: Configurable) extends RscriptCommandLineFunction {
  protected var script: File = config("script", default = "stackedBar.R")

  @Input
  var input: File = _

  @Output
  var output: File = _

  var width: Option[Int] = config("width")
  var height: Option[Int] = config("height")
  var xlabel: Option[String] = config("xlabel")
  var ylabel: Option[String] = config("ylabel")
  var llabel: Option[String] = config("llabel")
  var title: Option[String] = config("title")

  override def cmdLine: String = super.cmdLine +
    required("--input", input) +
    required("--output", output) +
    optional("--width", width) +
    optional("--height", height) +
    optional("--xlabel", xlabel) +
    required("--ylabel", ylabel) +
    optional("--llabel", llabel) +
    optional("--title", title)
}
