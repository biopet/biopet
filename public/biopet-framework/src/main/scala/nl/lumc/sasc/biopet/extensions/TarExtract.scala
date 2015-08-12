package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Created by pjvan_thof on 8/11/15.
 */
class TarExtract(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(required = true)
  var inputTar: File = _

  @Argument(required = true)
  var outputDir: File = _

  executable = config("exe", default = "tar", freeVar = false)
  override def versionCommand = executable + " --version"
  override def versionRegex = """tar \(GNU tar\) (.*)""".r

  override def beforeGraph: Unit = {
    super.beforeGraph
    jobLocalDir = outputDir
    jobOutputFile = new File(outputDir, "." + inputTar.getName + ".tar.out")
  }

  def cmdLine: String = required(executable) +
    required("-x") +
    required("-f", inputTar)
}
