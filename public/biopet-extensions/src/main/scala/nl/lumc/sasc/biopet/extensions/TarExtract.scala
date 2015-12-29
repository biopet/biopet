package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input }

/**
 * Created by pjvan_thof on 8/11/15.
 */
class TarExtract(val root: Configurable) extends BiopetCommandLineFunction with Version {
  @Input(required = true)
  var inputTar: File = _

  @Argument(required = true)
  var outputDir: File = _

  executable = config("exe", default = "tar", freeVar = false)
  def versionCommand = executable + " --version"
  def versionRegex = """tar \(GNU tar\) (.*)""".r

  override def beforeGraph: Unit = {
    super.beforeGraph
    jobLocalDir = outputDir
    jobOutputFile = new File(outputDir, "." + inputTar.getName + ".tar.out")
  }

  def cmdLine: String = required(executable) +
    required("-x") +
    required("-f", inputTar) +
    required("--directory", outputDir)
}
