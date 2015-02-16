package nl.lumc.sasc.biopet.extensions.bwa

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 1/16/15.
 */
class BwaAln(val root: Configurable) extends Bwa {
  @Input(doc = "Fastq file", required = true)
  var fastq: File = _

  @Input(doc = "The reference file for the bam files.", required = true)
  var reference: File = config("reference")

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

  override val defaultVmem = "5G"
  override val defaultThreads = 8

  def cmdLine = required(executable) +
    required("aln") +
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
