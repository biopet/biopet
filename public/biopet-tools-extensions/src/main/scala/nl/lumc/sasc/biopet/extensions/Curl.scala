package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.Output

/**
 * Created by pjvan_thof on 8/11/15.
 */
class Curl(val root: Configurable) extends BiopetCommandLineFunction {
  @Output
  var output: File = _

  var url: String = _

  executable = config("exe", default = "curl")
  override def versionCommand = executable + " --version"
  override def versionRegex = """curl (\w+\.\w+\.\w+) .*""".r

  def cmdLine: String = required(executable) + required(url) + " > " + required(output)
}
