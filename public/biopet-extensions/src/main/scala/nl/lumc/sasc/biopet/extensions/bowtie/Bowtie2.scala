package nl.lumc.sasc.biopet.extensions.bowtie

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Reference, Version }
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Extension for bowtie 2
 *
 * Based on version 2.2.6
 */
class Bowtie2(val root: Configurable) extends BiopetCommandLineFunction with Reference with Version {
  @Input(doc = "Fastq file R1", shortName = "R1")
  var R1: File = null

  @Input(doc = "Fastq file R2", shortName = "R2", required = false)
  var R2: Option[File] = None

  @Output(doc = "Output file SAM", shortName = "output", required = true)
  var output: File = null

  executable = config("exe", default = "bowtie2", freeVar = false)
  def versionRegex = """.*[Vv]ersion:? (.*)""".r
  override def versionExitcode = List(0, 1)
  def versionCommand = executable + " --version"

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
  var int_quals: Boolean = config("int_quals", default = false)

  /* Alignment options */
  var N: Option[Int] = config("N")
  var L: Option[Int] = config("L")
  var i: Option[String] = config("i")
  var n_ceil: Option[String] = config("n_ceil")
  var dpad: Option[Int] = config("dpad")
  var gbar: Option[Int] = config("gbar")
  var ignore_quals: Boolean = config("ignore_quals", default = false)
  var nofw: Boolean = config("nofw", default = false)
  var norc: Boolean = config("norc", default = false)
  var no_1mm_upfront: Boolean = config("no_1mm_upfront", default = false)
  var end_to_end: Boolean = config("end_to_end", default = false)
  var local: Boolean = config("local", default = false)

  /* Scoring */
  var ma: Option[Int] = config("ma")
  var mp: Option[Int] = config("mp")
  var np: Option[Int] = config("np")
  var rdg: Option[String] = config("rdg")
  var rfg: Option[String] = config("rfg")
  var score_min: Option[String] = config("score_min")

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
  var no_mixed: Boolean = config("no_mixed", default = false)
  var no_discordant: Boolean = config("no_discordant", default = false)
  var no_dovetail: Boolean = config("no_dovetail", default = false)
  var no_contain: Boolean = config("no_contain", default = false)
  var no_overlap: Boolean = config("no_overlap", default = false)

  /* Output */
  var time: Boolean = config("no_overlap", default = false)

  var un: Option[String] = config("un")
  var al: Option[String] = config("al")
  var un_conc: Option[String] = config("un_conc")
  var al_conc: Option[String] = config("al_conc")

  var un_gz: Option[String] = config("un_gz")
  var al_gz: Option[String] = config("al_gz")
  var un_conc_gz: Option[String] = config("un_conc_gz")
  var al_conc_gz: Option[String] = config("al_conc_gz")

  var un_bz2: Option[String] = config("un_bz2")
  var al_bz2: Option[String] = config("al_bz2")
  var un_conc_bz2: Option[String] = config("un_conc_bz2")
  var al_conc_bz2: Option[String] = config("al_conc_bz2")

  var quiet: Boolean = config("quiet", default = false)
  var met_file: Option[String] = config("met_file")
  var met_stderr: Boolean = config("met_stderr", default = false)
  var met: Option[Int] = config("met")

  var no_unal: Boolean = config("no_unal", default = false)
  var no_head: Boolean = config("no_head", default = false)
  var no_sq: Boolean = config("no_sq", default = false)

  var rg_id: Option[String] = config("rg_id")
  var rg: List[String] = config("rg", default = Nil)

  var omit_sec_seq: Boolean = config("omit_sec_seq", default = false)

  /* Performance */
  var reorder: Boolean = config("reorder", default = false)
  var mm: Boolean = config("mm", default = false)

  /* Other */
  var qc_filter: Boolean = config("qc_filter", default = false)
  var seed: Option[Int] = config("seed")
  var non_deterministic: Boolean = config("non_deterministic", default = false)

  override def beforeGraph() {
    super.beforeGraph()
    val indexDir = new File(bowtieIndex).getParentFile
    val basename = bowtieIndex.stripPrefix(indexDir.getPath + File.separator)
    if (indexDir.exists()) {
      if (!indexDir.list().toList.filter(_.startsWith(basename)).exists(_.endsWith(".bt2")))
        Logging.addError(s"No index files found for bowtie2 in: $indexDir with basename: $basename")
    }
  }

  /** return commandline to execute */
  def cmdLine = required(executable) +
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
    conditional(int_quals, "--int-quals") +
    /* Alignment options */
    optional("-N", N) +
    optional("-L", L) +
    optional("-i", i) +
    optional("--n-ceil", n_ceil) +
    optional("--dpad", dpad) +
    optional("--gbar", gbar) +
    conditional(ignore_quals, "--ignore-quals") +
    conditional(nofw, "--nofw") +
    conditional(norc, "--norc") +
    conditional(no_1mm_upfront, "--no-1mm-upfront") +
    conditional(end_to_end, "--end-to-end") +
    conditional(local, "--local") +
    /* Scoring */
    optional("--ma", ma) +
    optional("--mp", mp) +
    optional("--np", np) +
    optional("--rdg", rdg) +
    optional("--rfg", rfg) +
    optional("--score-min", score_min) +
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
    conditional(no_mixed, "--no-mixed") +
    conditional(no_discordant, "--no-discordant") +
    conditional(no_dovetail, "--no-dovetail") +
    conditional(no_contain, "--no-contain") +
    conditional(no_overlap, "--no-overlap") +
    /* Output */
    conditional(time, "--time") +
    optional("--un", un) +
    optional("--al", al) +
    optional("--un-conc", un_conc) +
    optional("--al-conc", al_conc) +
    optional("--un-gz", un_gz) +
    optional("--al-gz", al_gz) +
    optional("--un-conc-gz", un_conc_gz) +
    optional("--al-conc-gz", al_conc_gz) +
    optional("--un-bz2", un_bz2) +
    optional("--al-bz2", al_bz2) +
    optional("--un-conc-bz2", un_conc_bz2) +
    optional("--al-conc-bz2", al_conc_bz2) +
    conditional(quiet, "--quiet") +
    optional("--met-file", met_file) +
    conditional(met_stderr, "--met-stderr") +
    optional("--met", met) +
    conditional(no_unal, "--no-unal") +
    conditional(no_head, "--no-head") +
    conditional(no_sq, "--no-sq") +
    optional("--rg-id", rg_id) +
    repeat("--rg", rg) +
    conditional(omit_sec_seq, "--omit-sec-seq") +
    /* Performance */
    optional("--threads", threads) +
    conditional(reorder, "--reorder") +
    conditional(mm, "--mm") +
    /* Other */
    conditional(qc_filter, "--qc-filter") +
    optional("--seed", seed) +
    conditional(non_deterministic, "--non-deterministic") +
    /* Required */
    required("-x", bowtieIndex) +
    (R2 match {
      case Some(r2) =>
        required("-1", R1) +
          optional("-2", r2)
      case _ => required("-U", R1)
    }) +
    (if (outputAsStsout) "" else required("-S", output))
}
