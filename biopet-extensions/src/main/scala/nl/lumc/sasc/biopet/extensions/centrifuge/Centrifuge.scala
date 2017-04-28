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
package nl.lumc.sasc.biopet.extensions.centrifuge

import java.io.File

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.tryToParseNumber
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.io.Source
import scala.util.matching.Regex

/**
  * Created by pjvanthof on 19/09/16.
  */
class Centrifuge(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Version
    with Summarizable {
  @Input(doc = "Input: FastQ or FastA", required = true)
  var inputR1: File = _

  @Input(doc = "Input: FastQ or FastA", required = false)
  var inputR2: Option[File] = None

  var index: File = config("centrifuge_index")

  @Output(doc = "Output with hits per sequence")
  var output: File = _

  @Output(doc = "Output with hits per sequence")
  var report: Option[File] = None

  @Output(required = false)
  var un: Option[File] = None

  @Output(required = false)
  var al: Option[File] = None

  @Output(required = false)
  var unConc: Option[File] = None

  @Output(required = false)
  var alConc: Option[File] = None

  @Output(required = false)
  var metFile: Option[File] = None

  // Input args
  var q: Boolean = config("q", default = false)
  var qseq: Boolean = config("qseq", default = false)
  var f: Boolean = config("f", default = false)
  var r: Boolean = config("r", default = false)
  var c: Boolean = config("c", default = false)
  var skip: Option[Int] = config("skip")
  var upto: Option[Int] = config("upto")
  var trim5: Option[Int] = config("trim5")
  var trim3: Option[Int] = config("trim3")
  var phred33: Boolean = config("phred33", default = false)
  var phred64: Boolean = config("phred64", default = false)
  var intQuals: Boolean = config("int_quals", default = false)
  var ignoreQuals: Boolean = config("ignore_quals", default = false)
  var nofw: Boolean = config("nofw", default = false)
  var norc: Boolean = config("norc", default = false)

  // Classification args
  var minHitlen: Option[Int] = config("min_hitlen")
  var minTotallen: Option[Int] = config("min_totallen")
  var hostTaxids: List[Int] = config("host_taxids", default = Nil)
  var excludeTaxids: List[Int] = config("exclude_taxids", default = Nil)

  // Output args
  var t: Boolean = config("t", default = false)
  var quiet: Boolean = config("quiet", default = false)
  var metStderr: Boolean = config("met_stderr", default = false)
  var met: Option[Int] = config("met")

  override def defaultThreads = 8

  executable = config("exe", default = "centrifuge", freeVar = false)

  /** Command to get version of executable */
  def versionCommand: String = s"$executable --version"

  /** Regex to get version from version command output */
  def versionRegex: Regex = ".* version (.*)".r

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    deps :+= new File(index + ".1.cf")
    deps :+= new File(index + ".2.cf")
    deps :+= new File(index + ".3.cf")
  }

  /**
    * This function needs to be implemented to define the command that is executed
    *
    * @return Command to run
    */
  def cmdLine: String =
    executable +
      conditional(q, "-q") +
      conditional(qseq, "--qseq") +
      conditional(f, "-f") +
      conditional(r, "-r") +
      conditional(c, "-c") +
      optional("--skip", skip) +
      optional("--upto", upto) +
      optional("--trim5", trim5) +
      optional("--trim3", trim3) +
      conditional(phred33, "--phred33") +
      conditional(phred64, "--phred64") +
      conditional(intQuals, "--int-quals") +
      conditional(ignoreQuals, "--ignore-quals") +
      conditional(nofw, "--nofw") +
      conditional(norc, "--norc") +
      optional("--min-hitlen", minHitlen) +
      optional("--min-totallen", minTotallen) +
      optional("--host-taxids", if (hostTaxids.nonEmpty) Some(hostTaxids.mkString(",")) else None) +
      optional("--exclude-taxids",
               if (excludeTaxids.nonEmpty) Some(excludeTaxids.mkString(",")) else None) +
      optional("--met-file", metFile) +
      conditional(t, "-t") +
      conditional(quiet, "--quiet") +
      conditional(metStderr, "--met-stderr") +
      optional("--met", met) +
      optional(if (un.exists(_.getName.endsWith(".gz"))) "--un-gz" else "--un", un) +
      optional(if (al.exists(_.getName.endsWith(".gz"))) "--al-gz" else "--al", al) +
      optional(if (unConc.exists(_.getName.endsWith(".gz"))) "--un-conc-gz" else "--un-conc",
               unConc) +
      optional(if (alConc.exists(_.getName.endsWith(".gz"))) "--al-conc-gz" else "--al-conc",
               alConc) +
      optional("--threads", threads) +
      required("-x", index) +
      (inputR2 match {
        case Some(r2) => required("-1", inputR1) + required("-2", r2)
        case _ => required("-U", inputR1)
      }) +
      (if (outputAsStsout) "" else required("-S", output)) +
      optional("--report-file", report)

  /** Must return files to store into summary */
  override def summaryFiles: Map[String, File] = metFile.map("metrics" -> _).toMap

  /** Must returns stats to store into summary */
  override def summaryStats: Any = {
    metFile
      .map { file =>
        val reader = Source.fromFile(file)
        val header = reader.getLines().next().split("\t")
        val values = reader.getLines().next().split("\t").map(tryToParseNumber(_, true).get)
        reader.close()
        Map("metrics" -> header.zip(values).toMap)
      }
      .getOrElse(Map())
  }
}
