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

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Reference, Version}
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

/**
  * Extension for bowtie 2
  *
  * Based on version 2.2.6
  */
class Bowtie2(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Reference
    with Version {
  @Input(doc = "Fastq file R1", shortName = "R1")
  var R1: File = _

  @Input(doc = "Fastq file R2", shortName = "R2", required = false)
  var R2: Option[File] = None

  @Output(doc = "Output file SAM", shortName = "output", required = true)
  var output: File = _

  executable = config("exe", default = "bowtie2", freeVar = false)
  def versionRegex: List[Regex] = """.*[Vv]ersion:? (.*)""".r :: Nil
  override def versionExitcode = List(0, 1)
  def versionCommand: String = executable + " --version"

  override def defaultCoreMemory = 4.0
  override def defaultThreads = 4

  /* Required */
  var bowtieIndex: String = config("bowtie_index")

  /* Input options */
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

  /* Alignment options */
  var N: Option[Int] = config("N")
  var L: Option[Int] = config("L")
  var i: Option[String] = config("i")
  var nCeil: Option[String] = config("n_ceil")
  var dpad: Option[Int] = config("dpad")
  var gbar: Option[Int] = config("gbar")
  var ignoreQuals: Boolean = config("ignore_quals", default = false)
  var nofw: Boolean = config("nofw", default = false)
  var norc: Boolean = config("norc", default = false)
  var no1MmUpfront: Boolean = config("no_1mm_upfront", default = false)
  var endToEnd: Boolean = config("end_to_end", default = false)
  var local: Boolean = config("local", default = false)

  /* Scoring */
  var ma: Option[Int] = config("ma")
  var mp: Option[Int] = config("mp")
  var np: Option[Int] = config("np")
  var rdg: Option[String] = config("rdg")
  var rfg: Option[String] = config("rfg")
  var scoreMin: Option[String] = config("score_min")

  /* Reporting */
  var k: Option[Int] = config("k")
  var all: Option[Int] = config("all")

  /* Effort */
  var D: Option[Int] = config("D")
  var R: Option[Int] = config("R")

  /* Paired-end */
  var minins: Option[Int] = config("minins")
  var maxins: Option[Int] = config("maxins")
  var fr: Boolean = config("fr", default = false)
  var rf: Boolean = config("rf", default = false)
  var ff: Boolean = config("ff", default = false)
  var noMixed: Boolean = config("no_mixed", default = false)
  var noDiscordant: Boolean = config("no_discordant", default = false)
  var noDovetail: Boolean = config("no_dovetail", default = false)
  var noContain: Boolean = config("no_contain", default = false)
  var noOverlap: Boolean = config("no_overlap", default = false)

  /* Output */
  var time: Boolean = config("no_overlap", default = false)

  var un: Option[String] = config("un")
  var al: Option[String] = config("al")
  var unConc: Option[String] = config("un_conc")
  var alConc: Option[String] = config("al_conc")

  var unGz: Option[String] = config("un_gz")
  var alGz: Option[String] = config("al_gz")
  var unConcGz: Option[String] = config("un_conc_gz")
  var alConcGz: Option[String] = config("al_conc_gz")

  var unBz2: Option[String] = config("un_bz2")
  var alBz2: Option[String] = config("al_bz2")
  var unConcBz2: Option[String] = config("un_conc_bz2")
  var alConcBz2: Option[String] = config("al_conc_bz2")

  var quiet: Boolean = config("quiet", default = false)
  var metFile: Option[String] = config("met_file")
  var metStderr: Boolean = config("met_stderr", default = false)
  var met: Option[Int] = config("met")

  var noUnal: Boolean = config("no_unal", default = false)
  var noHead: Boolean = config("no_head", default = false)
  var noSq: Boolean = config("no_sq", default = false)

  var rgId: Option[String] = config("rg_id")
  var rg: List[String] = config("rg", default = Nil)

  var omitSecSeq: Boolean = config("omit_sec_seq", default = false)

  /* Performance */
  var reorder: Boolean = config("reorder", default = false)
  var mm: Boolean = config("mm", default = false)

  /* Other */
  var qcFilter: Boolean = config("qc_filter", default = false)
  var seed: Option[Int] = config("seed")
  var nonDeterministic: Boolean = config("non_deterministic", default = false)

  override def beforeGraph() {
    super.beforeGraph()
    val indexDir = new File(bowtieIndex).getParentFile
    val basename = bowtieIndex.stripPrefix(indexDir.getPath + File.separator)
    if (indexDir.exists()) {
      if (indexDir.canRead && indexDir.canExecute) {
        if (!indexDir
              .list()
              .toList
              .filter(_.startsWith(basename))
              .exists({ p =>
                p.endsWith(".bt2") || p.endsWith(".bt2l")
              }))
          Logging.addError(
            s"No index files found for bowtie2 in: $indexDir with basename: $basename")
      } else Logging.addError(s"Index dir of bowtie2 is not readable: $indexDir")
    }
  }

  /** return commandline to execute */
  def cmdLine: String =
    required(executable) +
      conditional(q, "-q") +
      conditional(qseq, "--qseq") +
      conditional(f, "-f") +
      conditional(r, "-r") +
      conditional(c, "-c") +
      optional("--skip", skip) +
      optional("--upto", upto) +
      optional("--trim3", trim3) +
      optional("--trim5", trim5) +
      conditional(phred33, "--phred33") +
      conditional(phred64, "--phred64") +
      conditional(intQuals, "--int-quals") +
      /* Alignment options */
      optional("-N", N) +
      optional("-L", L) +
      optional("-i", i) +
      optional("--n-ceil", nCeil) +
      optional("--dpad", dpad) +
      optional("--gbar", gbar) +
      conditional(ignoreQuals, "--ignore-quals") +
      conditional(nofw, "--nofw") +
      conditional(norc, "--norc") +
      conditional(no1MmUpfront, "--no-1mm-upfront") +
      conditional(endToEnd, "--end-to-end") +
      conditional(local, "--local") +
      /* Scoring */
      optional("--ma", ma) +
      optional("--mp", mp) +
      optional("--np", np) +
      optional("--rdg", rdg) +
      optional("--rfg", rfg) +
      optional("--score-min", scoreMin) +
      /* Reporting */
      optional("-k", k) +
      optional("--all", all) +
      /* Effort */
      optional("-D", D) +
      optional("-R", R) +
      /* Paired End */
      optional("--minins", minins) +
      optional("--maxins", maxins) +
      conditional(fr, "--fr") +
      conditional(rf, "--rf") +
      conditional(ff, "--ff") +
      conditional(noMixed, "--no-mixed") +
      conditional(noDiscordant, "--no-discordant") +
      conditional(noDovetail, "--no-dovetail") +
      conditional(noContain, "--no-contain") +
      conditional(noOverlap, "--no-overlap") +
      /* Output */
      conditional(time, "--time") +
      optional("--un", un) +
      optional("--al", al) +
      optional("--un-conc", unConc) +
      optional("--al-conc", alConc) +
      optional("--un-gz", unGz) +
      optional("--al-gz", alGz) +
      optional("--un-conc-gz", unConcGz) +
      optional("--al-conc-gz", alConcGz) +
      optional("--un-bz2", unBz2) +
      optional("--al-bz2", alBz2) +
      optional("--un-conc-bz2", unConcBz2) +
      optional("--al-conc-bz2", alConcBz2) +
      conditional(quiet, "--quiet") +
      optional("--met-file", metFile) +
      conditional(metStderr, "--met-stderr") +
      optional("--met", met) +
      conditional(noUnal, "--no-unal") +
      conditional(noHead, "--no-head") +
      conditional(noSq, "--no-sq") +
      optional("--rg-id", rgId) +
      repeat("--rg", rg) +
      conditional(omitSecSeq, "--omit-sec-seq") +
      /* Performance */
      optional("--threads", threads) +
      conditional(reorder, "--reorder") +
      conditional(mm, "--mm") +
      /* Other */
      conditional(qcFilter, "--qc-filter") +
      optional("--seed", seed) +
      conditional(nonDeterministic, "--non-deterministic") +
      /* Required */
      required("-x", bowtieIndex) +
      (R2 match {
        case Some(r2) =>
          required("-1", R1) +
            optional("-2", r2)
        case _ => required("-U", R1)
      }) +
      (if (outputAsStdout) "" else required("-S", output))
}
