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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Extension for bowtie 1
 *
 * Based on version 1.1.1
 */
class Bowtie(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Fastq file R1", shortName = "R1")
  var R1: File = null

  @Input(doc = "Fastq file R2", shortName = "R2", required = false)
  var R2: Option[File] = None

  @Input(doc = "The reference file for the bam files.", shortName = "R", required = true)
  var reference: File = config("reference")

  @Output(doc = "Output file SAM", shortName = "output", required = true)
  var output: File = null

  executable = config("exe", default = "bowtie", freeVar = false)
  override val versionRegex = """.*[Vv]ersion:? (.*)""".r
  override val versionExitcode = List(0, 1)
  override def versionCommand = executable + " --version"

  override val defaultCoreMemory = 4.0
  override val defaultThreads = 8

  var sam: Boolean = config("sam", default = true)
  var sam_RG: Option[String] = config("sam-RG")
  var seedlen: Option[Int] = config("seedlen")
  var seedmms: Option[Int] = config("seedmms")
  var k: Option[Int] = config("k")
  var m: Option[Int] = config("m")
  var best: Boolean = config("best", default = false)
  var maxbts: Option[Int] = config("maxbts")
  var strata: Boolean = config("strata", default = false)
  var maqerr: Option[Int] = config("maqerr")
  var maxins: Option[Int] = config("maxins")

  /** return commandline to execute */
  def cmdLine = {
    required(executable) +
      optional("--threads", threads) +
      conditional(sam, "--sam") +
      conditional(best, "--best") +
      conditional(strata, "--strata") +
      optional("--sam-RG", sam_RG) +
      optional("--seedlen", seedlen) +
      optional("--seedmms", seedmms) +
      optional("-k", k) +
      optional("-m", m) +
      optional("--maxbts", maxbts) +
      optional("--maqerr", maqerr) +
      optional("--maxins", maxins) +
      required(reference) +
      (R2 match {
        case Some(r2) => {
          required("-1", R1) +
            optional("-2", r2)
        }
        case _ => required(R1)
      }) +
      " > " + required(output)
  }
}