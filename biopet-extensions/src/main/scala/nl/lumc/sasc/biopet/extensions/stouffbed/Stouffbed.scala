package nl.lumc.sasc.biopet.extensions.stouffbed

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import org.broadinstitute.gatk.utils.commandline.Input

import scala.util.matching.Regex

/**
  * Created by Sander Bollen on 24-4-17.
  */
abstract class Stouffbed extends BiopetCommandLineFunction with Version {
  executable = config("exe", namespace = "stouffbed", default = "stouffbed")

  @Input
  var inputFiles: List[File] = Nil

  def versionCommand: String = executable + " --version"
  def versionRegex: List[Regex] = """.+, version (.*)""".r :: Nil

}
