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
package nl.lumc.sasc.biopet.extensions.hisat

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Reference, Version}
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

/**
  * Extension for hisat2
  *
  * Based on version 2.0.4
  */
class Hisat2(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Reference
    with Version {

  // TODO: handle --sra-acc flag. This is currently unsupported by the wrapper.

  @Input(doc = "Fastq file R1", shortName = "R1")
  var R1: File = _

  @Input(doc = "Fastq file R2", shortName = "R2", required = false)
  var R2: Option[File] = None

  @Output(doc = "Output file SAM", shortName = "output", required = true)
  var output: File = _

  executable = config("exe", default = "hisat2", freeVar = false)
  def versionRegex: Regex = """.*hisat2-align-s version (.*)""".r
  override def versionExitcode = List(0, 1)
  def versionCommand: String = executable + " --version"

  override def defaultCoreMemory = 4.0
  override def defaultThreads = 4

  /* Required */
  var hisat2Index: String = config("hisat2_index")

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
  var nCeil: Option[String] = config("n_ceil")
  var ignoreQuals: Boolean = config("ignore_quals", default = false)
  var nofw: Boolean = config("nofw", default = false)
  var norc: Boolean = config("norc", default = false)

  /* Spliced alignment */
  var penCansplice: Option[Int] = config("pen_cansplice")
  var penNoncansplice: Option[Int] = config("pen_noncansplice")
  var penCanintronlen: Option[Int] = config("pen_canintronlen")
  var penNoncanintronlen: Option[Int] = config("pen_noncanintronlen")
  var minIntronlen: Option[Int] = config("min_intronlen")
  var maxIntronlen: Option[Int] = config("max_intronlen")
  var knownSplicesiteInfile: Option[File] = config("known_splicesite_infile")
  var novelSplicesiteOutfile: Option[File] = config("novel_splicesite_outfile")
  var novelSplicesiteInfile: Option[File] = config("novel_splicesite_infile")
  var noTempSplicesite: Boolean = config("no_temp_splicesite", default = false)
  var noSplicedAlignment: Boolean = config("no_spliced_alignment", default = false)
  var rnaStrandness: Option[String] = config("rna_strandness")
  var tmo: Boolean = config("tmo", default = false)
  var dta: Boolean = config("dta", default = false)
  var dtaCufflinks: Boolean = config("dta_cufflinks", default = false)

  /* Scoring */
  var ma: Option[Int] = config("ma")
  var mp: Option[String] = config("mp") // TODO: should be (Int, Int)
  var sp: Option[String] = config("sp") // TODO: should be (Int, Int)
  var np: Option[Int] = config("np")
  var rdg: Option[String] = config("rdg") // TODO: should be (Int, Int)
  var rfg: Option[String] = config("rfg") // TODO: should be (Int, Int)
  var scoreMin: Option[String] = config("score_min")

  /* Reporting */
  var k: Option[Int] = config("k")
  var all: Option[Int] = config("all")

  /* Paired-end */
  var fr: Boolean = config("fr", default = false)
  var rf: Boolean = config("rf", default = false)
  var ff: Boolean = config("ff", default = false)
  var noMixed: Boolean = config("no_mixed", default = false)
  var noDiscordant: Boolean = config("no_discordant", default = false)

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

  var noHead: Boolean = config("no_head", default = false)
  var noSq: Boolean = config("no_sq", default = false)

  var rgId: Option[String] = config("rg_id")
  var rg: List[String] = config("rg", default = Nil)

  var omitSecSeq: Boolean = config("omit_sec_seq", default = false)

  /* Performance */
  var offrate: Option[Int] = config("offrate")
  var reorder: Boolean = config("reorder", default = false)
  var mm: Boolean = config("mm", default = false)

  /* Other */
  var qcFilter: Boolean = config("qc_filter", default = false)
  var seed: Option[Int] = config("seed")
  var nonDeterministic: Boolean = config("non_deterministic", default = false)
  var removeChrName: Boolean = config("remove_chrname", default = false)
  var addChrName: Boolean = config("add_chrname", default = false)

  override def beforeGraph() {
    super.beforeGraph()
    val indexDir = new File(hisat2Index).getParentFile
    val basename = hisat2Index.stripPrefix(indexDir.getPath + File.separator)
    if (indexDir.exists()) {
      if (!indexDir
            .list()
            .toList
            .filter(_.startsWith(basename))
            .exists(_.endsWith(".ht2")))
        Logging.addError(s"No index files found for hisat2 in: $indexDir with basename: $basename")
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
      optional("--n-ceil", nCeil) +
      conditional(ignoreQuals, "--ignore-quals") +
      conditional(nofw, "--nofw") +
      conditional(norc, "--norc") +
      /* Spliced alignment */
      optional("--pen-cansplice", penCansplice) +
      optional("--pen-noncansplice", penNoncansplice) +
      optional("--pen-canintronlen", penCanintronlen) +
      optional("--pen-noncanintronlen", penNoncanintronlen) +
      optional("--min-intronlen", minIntronlen) +
      optional("--max-intronlen", maxIntronlen) +
      optional("--known-splicesite-infile", knownSplicesiteInfile) +
      optional("--novel-splicesite-outfile", novelSplicesiteOutfile) +
      optional("--novel-splicesite-infile", novelSplicesiteInfile) +
      conditional(noTempSplicesite, "--no-temp-splicesite") +
      conditional(noSplicedAlignment, "--no-spliced-alignment") +
      optional("--rna-strandness", rnaStrandness) +
      conditional(tmo, "--tmo") +
      conditional(dta, "--dta") +
      conditional(dtaCufflinks, "--dta-cufflinks") +
      /* Scoring */
      optional("--ma", ma) +
      optional("--mp", mp) +
      optional("--np", np) +
      optional("--sp", sp) +
      optional("--rdg", rdg) +
      optional("--rfg", rfg) +
      optional("--score-min", scoreMin) +
      /* Reporting */
      optional("-k", k) +
      optional("--all", all) +
      /* Paired End */
      conditional(fr, "--fr") +
      conditional(rf, "--rf") +
      conditional(ff, "--ff") +
      conditional(noMixed, "--no-mixed") +
      conditional(noDiscordant, "--no-discordant") +
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
      conditional(noHead, "--no-head") +
      conditional(noSq, "--no-sq") +
      optional("--rg-id", rgId) +
      repeat("--rg", rg) +
      conditional(omitSecSeq, "--omit-sec-seq") +
      /* Performance */
      optional("--offrate", offrate) +
      optional("--threads", threads) +
      conditional(reorder, "--reorder") +
      conditional(mm, "--mm") +
      /* Other */
      conditional(qcFilter, "--qc-filter") +
      optional("--seed", seed) +
      conditional(nonDeterministic, "--non-deterministic") +
      conditional(removeChrName, "--remove-chrname") +
      conditional(addChrName, "--add-chrname") +
      /* Required */
      required("-x", hisat2Index) +
      (R2 match {
        case Some(r2) => required("-1", R1) + optional("-2", r2)
        case _ => required("-U", R1)
      }) +
      (if (outputAsStdout) "" else required("-S", output))
}
