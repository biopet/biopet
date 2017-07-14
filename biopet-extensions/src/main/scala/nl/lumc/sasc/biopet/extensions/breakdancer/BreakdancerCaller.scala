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
package nl.lumc.sasc.biopet.extensions.breakdancer

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

class BreakdancerCaller(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "breakdancer-max", freeVar = false)

  override def defaultThreads = 1 // breakdancer can only work on 1 single thread

  def versionRegex: Regex = """.*[Vv]ersion:? (.*)""".r
  override def versionExitcode = List(1)
  def versionCommand: String = executable

  @Input(doc = "The breakdancer configuration file")
  var input: File = _

  //  @Argument(doc = "Work directory")
  //  var workdir: String = _

  @Output(doc = "Breakdancer TSV output")
  var output: File = _

  /*
   Options:
       -o STRING       operate on a single chromosome [all chromosome]
       -s INT          minimum length of a region [7]
       -c INT          cutoff in unit of standard deviation [3]
       -m INT          maximum SV size [1000000000]
       -q INT          minimum alternative mapping quality [35]
       -r INT          minimum number of read pairs required to establish a connection [2]
       -x INT          maximum threshold of haploid sequence coverage for regions to be ignored [1000]
       -b INT          buffer size for building connection [100]
       -t              only detect transchromosomal rearrangement, by default off
       -d STRING       prefix of fastq files that SV supporting reads will be saved by library
       -g STRING       dump SVs and supporting reads in BED format for GBrowse
       -l              analyze Illumina long insert (mate-pair) library
       -a              print out copy number and support reads per library rather than per bam, by default off
       -h              print out Allele Frequency column, by default off
       -y INT          output score filter [30]
   */

  var s: Option[Int] = config("s")
  var c: Option[Int] = config("c")
  var m: Option[Int] = config("m")
  var q: Option[Int] = config("qs")
  var r: Option[Int] = config("r")
  var x: Option[Int] = config("x")
  var b: Option[Int] = config("b")
  var t: Boolean = config("t", default = false)
  var d: Option[String] = config("d")
  var g: Option[String] = config("g")
  var l: Boolean = config("l", default = false)
  var a: Boolean = config("a", default = false)
  var h: Boolean = config("h", default = false)
  var y: Option[Int] = config("y")

  override def beforeCmd() {}

  def cmdLine: String =
    required(executable) +
      optional("-s", s) +
      optional("-c", c) +
      optional("-m", m) +
      optional("-q", q) +
      optional("-r", r) +
      optional("-x", x) +
      optional("-b", b) +
      conditional(t, "-t") +
      optional("-d", d) +
      optional("-g", g) +
      conditional(l, "-l") +
      conditional(a, "-a") +
      conditional(h, "-h") +
      optional("-y", y) +
      required(input) +
      ">" +
      required(output)
}

object BreakdancerCaller {
  def apply(root: Configurable, input: File, output: File): BreakdancerCaller = {
    val bdcaller = new BreakdancerCaller(root)
    bdcaller.input = input
    bdcaller.output = output
    bdcaller
  }
}
