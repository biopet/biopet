
package nl.lumc.sasc.biopet.extensions.igvtools

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File

class IGVToolsCount(val root: Configurable) extends IGVTools {
  @Input(doc = "Bam File")
  var input: File = _

  @Argument(doc = "Genome name")
  var genomename: String = _

  @Output(doc = "output File")
  var output: File = _

  def cmdLine = required(executable) + required("count") + required(input) + required(output) + required(genomename)
}

object IGVToolsCount {
  def apply(root: Configurable, input: File, output: File, genomename: String): IGVToolsCount = {
    val counting = new IGVToolsCount(root)
    counting.input = input
    counting.output = output
    counting.genomename = genomename
    return counting
  }

  def apply(root: Configurable, input: File, genomename: String): IGVToolsCount = {
    return apply(root, input, new File(swapExtension(input.getCanonicalPath)), genomename)
  }

  private def swapExtension(inputFile: String) = inputFile + ".tdf"
}