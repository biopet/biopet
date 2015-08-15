package nl.lumc.sasc.biopet.extensions.bowtie

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input }

/**
 * Created by pjvan_thof on 8/15/15.
 */
class BowtieBuild(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(required = true)
  var reference: File = _

  @Argument(required = true)
  var baseName: String = _

  executable = config("exe", default = "bowtie-build", freeVar = false)
  override def versionRegex = """.*[Vv]ersion:? (\d*\.\d*\.\d*)""".r
  override def versionCommand = executable + " --version"

  override def defaultCoreMemory = 15.0

  def cmdLine = required(executable) +
    required(reference) +
    required(baseName)
}
