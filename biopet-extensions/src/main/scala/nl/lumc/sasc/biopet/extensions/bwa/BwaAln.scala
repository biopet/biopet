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
package nl.lumc.sasc.biopet.extensions.bwa

import java.io.File

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Extension for bwa aln
 *
 * Based on version 0.7.12-r1039
 *
 * Created by pjvan_thof on 1/16/15.
 */
class BwaAln(val parent: Configurable) extends Bwa with Reference {
  @Input(doc = "Fastq file", required = true)
  var fastq: File = _

  @Input(doc = "The reference file for the bam files.", required = true)
  var reference: File = null

  @Output(doc = "Output file SAM", required = false)
  var output: File = _

  var n: Option[Int] = config("n")
  var o: Option[Int] = config("o")
  var e: Option[Int] = config("e")
  var i: Option[Int] = config("i")
  var d: Option[Int] = config("d")
  var l: Option[Int] = config("l")
  var k: Option[Int] = config("k")
  var m: Option[Int] = config("m")
  var M: Option[Int] = config("M")
  var O: Option[Int] = config("O")
  var E: Option[Int] = config("E")
  var R: Option[Int] = config("R")
  var q: Option[Int] = config("q")
  var B: Option[Int] = config("B")
  var L: Boolean = config("L", default = false)
  var N: Boolean = config("N", default = false)
  var I: Boolean = config("I", default = false)
  var b: Boolean = config("b", default = false)
  var n0: Boolean = config("0", default = false)
  var n1: Boolean = config("1", default = false)
  var n2: Boolean = config("2", default = false)
  var Y: Boolean = config("Y", default = false)

  override def defaultCoreMemory = 4.0
  override def defaultThreads = 8

  override def beforeGraph() {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
  }

  /** Returns command to execute */
  def cmdLine = required(executable) +
    required("aln") +
    optional("-t", threads) +
    optional("-n", n) +
    optional("-o", o) +
    optional("-e", e) +
    optional("-i", i) +
    optional("-d", d) +
    optional("-l", l) +
    optional("-k", k) +
    optional("-m", m) +
    optional("-M", M) +
    optional("-O", O) +
    optional("-E", E) +
    optional("-R", R) +
    optional("-q", q) +
    optional("-B", B) +
    conditional(L, "-L") +
    conditional(N, "-N") +
    conditional(I, "-I") +
    conditional(b, "-b") +
    conditional(n0, "-0") +
    conditional(n1, "-1") +
    conditional(n2, "-2") +
    conditional(Y, "-Y") +
    optional("-f", output) +
    required(reference) +
    required(fastq)
}
