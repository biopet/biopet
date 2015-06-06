package nl.lumc.sasc.biopet.extensions.rscript

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.RscriptCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by pjvan_thof on 4/29/15.
 */
class XYPlot(val root: Configurable) extends RscriptCommandLineFunction {
  protected var script: File = config("script", default = "plotXY.R")

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
  var removeZero: Boolean = config("removeZero", default = false)

  override def cmdLine: String = super.cmdLine +
    required("--input", input) +
    required("--output", output) +
    optional("--width", width) +
    optional("--height", height) +
    optional("--xlabel", xlabel) +
    required("--ylabel", ylabel) +
    optional("--llabel", llabel) +
    optional("--title", title) +
    optional("--removeZero", removeZero)
}
