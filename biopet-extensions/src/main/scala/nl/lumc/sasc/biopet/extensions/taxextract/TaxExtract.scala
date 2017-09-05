package nl.lumc.sasc.biopet.extensions.taxextract

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import org.broadinstitute.gatk.utils.commandline.{Argument, Input}

import scala.util.matching.Regex

/**
  * Created by Sander Bollen on 2-5-17.
  */
abstract class TaxExtract extends BiopetCommandLineFunction with Version {

  executable = config("exe", namespace = "taxextract", default = "taxextract")

  def subCommand: String

  @Input
  var inputKreport: File = _

  @Argument(required = true, doc = "taxonomy name to extract")
  var taxName: String = config("taxonomy", namespace = "taxextract")

  def cmdLine: String = {
    executable +
      required(subCommand) +
      required("-i", inputKreport) +
      required("-n", taxName)
  }

  def versionCommand: String = executable + " --version"
  def versionRegex: List[Regex] = """.+, version (.*)""".r :: Nil
}
