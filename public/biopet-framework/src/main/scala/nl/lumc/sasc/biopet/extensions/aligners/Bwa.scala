/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions.aligners

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class Bwa(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Fastq file R1", shortName = "R1")
  var R1: File = _

  @Input(doc = "Fastq file R2", shortName = "R2", required = false)
  var R2: File = _

  @Input(doc = "The reference file for the bam files.", shortName = "R")
  var reference: File = config("reference", required = true)

  @Output(doc = "Output file SAM", shortName = "output")
  var output: File = _

  var R: String = config("R")
  var k: Option[Int] = config("k")
  var r: Option[Float] = config("r")
  var S: Boolean = config("S", default = false)
  var M: Boolean = config("M", default = true)
  var w: Option[Int] = config("w")
  var d: Option[Int] = config("d")
  var c: Option[Int] = config("c")
  var D: Option[Float] = config("D")
  var W: Option[Int] = config("W")
  var m: Option[Int] = config("m")
  var P: Boolean = config("P", default = false)
  var e: Boolean = config("e", default = false)
  var A: Option[Int] = config("A")
  var B: Option[Int] = config("B")
  var O: String = config("O")
  var E: String = config("E")
  var L: String = config("L")
  var U: Option[Int] = config("U")
  var x: String = config("x")
  var p: Boolean = config("p", default = false)
  var v: Option[Int] = config("v")
  var T: Option[Int] = config("T")
  var h: Option[Int] = config("h")
  var a: Boolean = config("a", default = false)
  var C: Boolean = config("C", default = false)
  var Y: Boolean = config("Y", default = false)
  var I: String = config("I")

  executable = config("exe", default = "bwa", freeVar = false)
  override val versionRegex = """Version: (.*)""".r
  override val versionExitcode = List(0, 1)

  override val defaultVmem = "6G"
  override val defaultThreads = 8

  override def versionCommand = executable

  def cmdLine = {
    required(executable) +
      required("mem") +
      optional("-k", k) +
      optional("-r", r) +
      optional("-t", nCoresRequest) +
      optional("-R", R) +
      conditional(M, "-M") +
      conditional(S, "-S") +
      optional("-w", w) +
      optional("-d", d) +
      optional("-c", c) +
      optional("-D", D) +
      optional("-W", W) +
      optional("-m", m) +
      conditional(P, "-P") +
      conditional(e, "-e") +
      optional("-A", A) +
      optional("-B", B) +
      optional("-O", O) +
      optional("-E", E) +
      optional("-L", L) +
      optional("-U", U) +
      optional("-x", x) +
      conditional(p, "-p") +
      optional("-v", v) +
      optional("-T", T) +
      optional("-h", h) +
      conditional(a, "-a") +
      conditional(C, "-C") +
      conditional(Y, "-Y") +
      optional("-I", I) +
      required(reference) +
      required(R1) +
      optional(R2) +
      " > " + required(output)
  }
}
