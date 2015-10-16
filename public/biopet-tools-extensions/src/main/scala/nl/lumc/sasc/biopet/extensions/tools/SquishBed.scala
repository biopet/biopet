package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFuntion
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by pjvanthof on 22/08/15.
 */
class SquishBed(val root: Configurable) extends ToolCommandFuntion {
  def toolObject = nl.lumc.sasc.biopet.tools.SquishBed

  @Input(doc = "Input Bed file", required = true)
  var input: File = _

  @Output(doc = "Output interval list", required = true)
  var output: File = _

  var strandSensitive: Boolean = config("strandSensitive", default = false)

  override def cmdLine = super.cmdLine +
    required("-I", input) +
    required("-o", output) +
    conditional(strandSensitive, "-s")
}
