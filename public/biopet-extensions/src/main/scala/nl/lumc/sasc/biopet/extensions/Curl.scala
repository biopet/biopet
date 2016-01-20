package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Output

/**
 * Created by pjvan_thof on 8/11/15.
 */
class Curl(val root: Configurable) extends BiopetCommandLineFunction with Version {
  @Output
  var output: File = _

  var url: String = _

  executable = config("exe", default = "curl")
  def versionCommand = executable + " --version"
  def versionRegex = """curl (\w+\.\w+\.\w+) .*""".r

  def cmdLine: String = required(executable) + required(url) + " > " + required(output)
}
