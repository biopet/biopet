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
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.extensions.bcftools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/** This extension is based on bcftools 1.1-134 */
class BcftoolsCall(val parent: Configurable) extends Bcftools {
  @Input(doc = "Input File", required = false)
  var input: File = _

  @Output(doc = "output File", required = false)
  var output: File = _

  var O: Option[String] = None
  var v: Boolean = config("v", default = false)
  var c: Boolean = config("c", default = false)
  var m: Boolean = config("m", default = false)
  var r: Option[String] = config("r")
  @Input(required = false)
  var R: Option[File] = config("R")
  var s: Option[String] = config("s")
  @Input(required = false)
  var S: Option[File] = config("S")
  var t: Option[String] = config("t")
  @Input(required = false)
  var T: Option[File] = config("T")
  var A: Boolean = config("A", default = false)
  var f: List[String] = config("f", default = Nil)
  var g: Option[Int] = config("g")
  var i: Boolean = config("i", default = false)
  var M: Boolean = config("M", default = false)
  var V: Option[String] = config("V")
  var C: Option[String] = config("C")
  var n: Option[Float] = config("n")
  var p: Option[Float] = config("p")
  var P: Option[Float] = config("P")
  var X: Boolean = config("X", default = false)
  var Y: Boolean = config("Y", default = false)

  override def beforeGraph(): Unit = {
    require(c != m)
  }

  def cmdLine: String =
    required(executable) +
      required("call") +
      optional("-O", O) +
      conditional(v, "-v") +
      conditional(c, "-c") +
      conditional(m, "-m") +
      optional("-r", r) +
      optional("-R", R) +
      optional("-s", s) +
      optional("-S", S) +
      optional("-t", t) +
      optional("-T", T) +
      conditional(A, "-A") +
      repeat("-f", f) +
      optional("-g", g) +
      conditional(i, "-i") +
      conditional(M, "-M") +
      optional("-V", V) +
      optional("-C", C) +
      optional("-n", n) +
      optional("-p", p) +
      optional("-P", P) +
      conditional(X, "-X") +
      conditional(Y, "-Y") +
      (if (outputAsStdout) "" else required("-o", output)) +
      (if (inputAsStdin) "-" else required(input))
}
