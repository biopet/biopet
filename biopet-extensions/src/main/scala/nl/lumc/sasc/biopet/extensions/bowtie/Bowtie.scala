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
package nl.lumc.sasc.biopet.extensions.bowtie

import java.io.File

import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Reference, Version}
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

/**
  * Extension for bowtie 1
  *
  * Based on version 1.1.1
  */
class Bowtie(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Reference
    with Version {
  @Input(doc = "Fastq file R1", shortName = "R1")
  var R1: File = _

  @Input(doc = "Fastq file R2", shortName = "R2", required = false)
  var R2: Option[File] = None

  @Output(doc = "Output file SAM", shortName = "output", required = true)
  var output: File = _

  executable = config("exe", default = "bowtie", freeVar = false)
  def versionRegex: Regex = """.*[Vv]ersion:? (.*)""".r
  override def versionExitcode = List(0, 1)
  def versionCommand: String = executable + " --version"

  override def defaultCoreMemory = 4.0
  override def defaultThreads = 8

  var sam: Boolean = config("sam", default = false)
  var samRg: Option[String] = config("sam-RG")
  var seedlen: Option[Int] = config("seedlen")
  var seedmms: Option[Int] = config("seedmms")
  var k: Option[Int] = config("k")
  var m: Option[Int] = config("m")
  var best: Boolean = config("best", default = false)
  var maxbts: Option[Int] = config("maxbts")
  var strata: Boolean = config("strata", default = false)
  var maqerr: Option[Int] = config("maqerr")
  var maxins: Option[Int] = config("maxins")
  var largeIndex: Boolean = config("large-index", default = false)
  var bowtieIndex: String = config("bowtie_index")

  override def beforeGraph() {
    super.beforeGraph()
    val indexDir = new File(bowtieIndex).getParentFile
    val basename = bowtieIndex.stripPrefix(indexDir.getPath + File.separator)
    if (indexDir.exists()) {
      if (indexDir.list().toList.filter(_.startsWith(basename)).exists(_.endsWith(".ebwtl")))
        largeIndex = config("large-index", default = true)
      else {
        if (!indexDir.list().toList.filter(_.startsWith(basename)).exists(_.endsWith(".ebwt")))
          Logging.addError(
            s"No index files found for bowtie in: $indexDir with basename: $basename")
      }
    }
    if (R2.nonEmpty && maxins.isEmpty) {
      Logging.addError(
        "The parameter 'maxins' that specifies the maximum allowed insert size, is missing in the configuration. Please note that Bowtie won't align reads coming from inserts longer than this value.")
    }
  }

  /** return commandline to execute */
  def cmdLine: String =
    required(executable) +
      optional("--threads", threads) +
      conditional(sam, "--sam") +
      conditional(largeIndex, "--large-index") +
      conditional(best, "--best") +
      conditional(strata, "--strata") +
      optional("--sam-RG", samRg) +
      optional("--seedlen", seedlen) +
      optional("--seedmms", seedmms) +
      optional("-k", k) +
      optional("-m", m) +
      optional("--maxbts", maxbts) +
      optional("--maqerr", maqerr) +
      optional("--maxins", maxins) +
      required(bowtieIndex) +
      (R2 match {
        case Some(r2) =>
          required("-1", R1) +
            optional("-2", r2)
        case _ => required(R1)
      }) +
      " > " + required(output)
}
