package nl.lumc.sasc.biopet.extensions.stringtie

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Reference, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

class Stringtie(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Reference
    with Version {

  executable = config("exe", "stringtie")

  @Input(required = true)
  var inputBam: File = _

  @Input(required = false)
  var referenceGtf: Option[File] = None

  @Output
  var outputGtf: File = _

  @Output
  var geneAbundances: Option[File] = None

  @Output
  var referenceCoverage: Option[File] = None

  var rf: Boolean = config("rf", default = false)
  var fr: Boolean = config("fr", default = false)

  var v: Boolean = config("v", default = logger.isDebugEnabled)
  var l: Option[String] = None
  var f: Option[Double] = config("f")
  var m: Option[Int] = config("m")
  var a: Option[Int] = config("a")
  var j: Option[Float] = config("j")
  var t: Boolean = config("t", default = false)
  var c: Option[Float] = config("c")
  var g: Option[Int] = config("g")
  var B: Boolean = config("B", default = false)
  var b: Option[String] = config("b")
  var e: Boolean = config("e", default = false)
  var M: Option[Float] = config("M")
  var x: List[String] = config("x", default = Nil)

  /** Command to get version of executable */
  def versionCommand: String = executable + " --version"

  /** Regex to get version from version command output */
  def versionRegex: Regex = "(.*)".r

  def cmdLine: String =
    required(executable) +
      required(inputBam) +
      conditional(v, "-v") +
      required("-p", threads) +
      conditional(rf, "--rf") +
      conditional(fr, "--fr") +
      optional("-l", l) +
      optional("-f", f) +
      optional("-m", m) +
      optional("-A", geneAbundances) +
      optional("-C", referenceCoverage) +
      optional("-a", a) +
      optional("-j", j) +
      conditional(t, "-t") +
      optional("-c", c) +
      optional("-g", g) +
      conditional(B, "-B") +
      optional("-b", b) +
      conditional(e, "-e") +
      optional("-M", M) +
      optional("-G", referenceGtf) +
      (if (x.nonEmpty) optional("-x", x.mkString(",")) else "") +
      (if (outputAsStdout) "" else required("-o", outputGtf))

}
