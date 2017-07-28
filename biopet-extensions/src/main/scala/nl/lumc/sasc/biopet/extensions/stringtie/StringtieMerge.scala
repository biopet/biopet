package nl.lumc.sasc.biopet.extensions.stringtie

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Reference, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

class StringtieMerge(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Reference
    with Version {

  executable = config("exe", "stringtie")

  @Input(required = true)
  var inputGtfs: List[File] = Nil

  @Input(required = false)
  var referenceGtf: Option[File] = None

  @Output
  var outputGtf: File = _

  var v: Boolean = config("v", default = logger.isDebugEnabled)
  var l: Option[String] = None
  var f: Option[Double] = config("f")
  var m: Option[Int] = config("m")
  var c: Option[Float] = config("c")
  var F: Option[Double] = config("F")
  var T: Option[Double] = config("T")
  var i: Boolean = config("i", default = false)

  /** Command to get version of executable */
  def versionCommand: String = executable + " --version"

  /** Regex to get version from version command output */
  def versionRegex: Regex = "(.*)".r

  def cmdLine: String =
    required(executable) +
      required("--merge") +
      conditional(v, "-v") +
      required("-p", threads) +
      optional("-l", l) +
      optional("-f", f) +
      optional("-m", m) +
      optional("-c", c) +
      optional("-F", F) +
      conditional(i, "-i") +
      optional("-G", referenceGtf) +
      (if (outputAsStdout) "" else required("-o", outputGtf)) +
      repeat(inputGtfs)

}
