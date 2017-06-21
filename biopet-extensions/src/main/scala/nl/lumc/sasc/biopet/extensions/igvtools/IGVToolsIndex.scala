package nl.lumc.sasc.biopet.extensions.igvtools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

class IGVToolsIndex(val parent: Configurable) extends IGVTools {

  @Input(required = true)
  var input: File = _

  override def cmdLine = super.cmdLine + required("index") + required(input)

}

object IGVToolsIndex {
  def apply(parent: Configurable, input: File): IGVToolsIndex = {
    val index = new IGVToolsIndex(parent)
    index.input = input
    index
  }
}