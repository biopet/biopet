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

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction, Reference }
import nl.lumc.sasc.biopet.utils.tryToParseNumber
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.io.Source

/**
 * Extension for VariantEffectPredictor
 * Created by ahbbollen on 15-1-15.
 */
class VariantEffectPredictor(val root: Configurable) extends BiopetCommandLineFunction with Reference with Version with Summarizable {

  executable = config("exe", submodule = "perl", default = "perl")
  var vepScript: String = config("vep_script")

  @Input(doc = "input VCF", required = true)
  var input: File = null

  @Output(doc = "output file", required = true)
  var output: File = null

  def versionRegex = """version (\d*)""".r
  def versionCommand = executable + " " + vepScript + " --help"

  //Boolean vars
  var v: Boolean = config("v", default = true, freeVar = false)
  var q: Boolean = config("q", default = false, freeVar = false)
  var offline: Boolean = config("offline", default = false)
  var no_progress: Boolean = config("no_progress", default = false)
  var everything: Boolean = config("everything", default = false)
  var force: Boolean = config("force", default = false)
  var no_stats: Boolean = config("no_stats", default = false)
  var stats_text: Boolean = config("stats_text", default = true)
  var html: Boolean = config("html", default = false)
  var cache: Boolean = config("cache", default = false)
  var humdiv: Boolean = config("humdiv", default = false)
  var regulatory: Boolean = config("regulatory", default = false)
  var cell_type: Boolean = config("cell_type", default = false)
  var phased: Boolean = config("phased", default = false)
  var allele_number: Boolean = config("allele_number", default = false)
  var numbers: Boolean = config("numbers", default = false)
  var domains: Boolean = config("domains", default = false)
  var no_escape: Boolean = config("no_escape", default = false)
  var hgvs: Boolean = config("hgvs", default = false)
  var protein: Boolean = config("protein", default = false)
  var symbol: Boolean = config("symbol", default = false)
  var ccds: Boolean = config("ccds", default = false)
  var uniprot: Boolean = config("uniprot", default = false)
  var tsl: Boolean = config("tsl", default = false)
  var canonical: Boolean = config("canonical", default = false)
  var biotype: Boolean = config("biotype", default = false)
  var xref_refseq: Boolean = config("xref_refseq", default = false)
  var check_existing: Boolean = config("check_existing", default = false)
  var check_alleles: Boolean = config("check_alleles", default = false)
  var check_svs: Boolean = config("svs", default = false)
  var gmaf: Boolean = config("gmaf", default = false)
  var maf_1kg: Boolean = config("maf_1kg", default = false)
  var maf_esp: Boolean = config("maf_esp", default = false)
  var old_map: Boolean = config("old_maf", default = false)
  var pubmed: Boolean = config("pubmed", default = false)

  var vcf: Boolean = config("vcf", default = true, freeVar = false)
  var json: Boolean = config("json", default = false, freeVar = false)
  var gvf: Boolean = config("gvf", default = false)
  var check_ref: Boolean = config("check_ref", default = false)
  var coding_only: Boolean = config("coding_only", default = false)
  var no_intergenic: Boolean = config("no_intergenic", default = false)
  var pick: Boolean = config("pick", default = false)
  var pick_allele: Boolean = config("pick_allele", default = false)
  var flag_pick: Boolean = config("flag_pick", default = false)
  var flag_pick_allele: Boolean = config("flag_pick_allele", default = false)
  var per_gene: Boolean = config("per_gene", default = false)
  var most_severe: Boolean = config("most_severe", default = false)
  var summary: Boolean = config("summary", default = false)
  var filter_common: Boolean = config("filter_common", default = false)
  var check_frequency: Boolean = config("check_frequency", default = false)
  var allow_non_variant: Boolean = config("allow_non_variant", default = false)
  var database: Boolean = config("database", default = false)
  var genomes: Boolean = config("genomes", default = false)
  var gencode_basic: Boolean = config("gencode_basic", default = false)
  var refseq: Boolean = config("refseq", default = false)
  var merged: Boolean = config("merged", default = false)
  var all_refseq: Boolean = config("all_refseq", default = false)
  var lrg: Boolean = config("lrg", default = false)
  var no_whole_genome: Boolean = config("no_whole_genome", default = false)
  var skip_db_check: Boolean = config("skip_db_check", default = false)

  // Textual args
  var vep_config: Option[String] = config("config", freeVar = false)
  var species: Option[String] = config("species", freeVar = false)
  var assembly: Option[String] = config("assembly")
  var format: Option[String] = config("format")
  var dir: Option[String] = config("dir")
  var dir_cache: Option[String] = config("dir_cache")
  var dir_plugins: Option[String] = config("dir_plugins")
  var fasta: Option[String] = config("fasta")
  var sift: Option[String] = config("sift")
  var polyphen: Option[String] = config("polyphen")
  var custom: Option[String] = config("custom")
  var plugin: List[String] = config("plugin", default = Nil)
  var individual: Option[String] = config("individual")
  var fields: Option[String] = config("fields")
  var convert: Option[String] = config("convert")
  var terms: Option[String] = config("terms")
  var chr: Option[String] = config("chr")
  var pick_order: Option[String] = config("pick_order")
  var freq_pop: Option[String] = config("check_pop")
  var freq_gt_lt: Option[String] = config("freq_gt_lt")
  var freq_filter: Option[String] = config("freq_filter")
  var filter: Option[String] = config("filter")
  var host: Option[String] = config("host")
  var user: Option[String] = config("user")
  var password: Option[String] = config("password")
  var registry: Option[String] = config("registry")
  var build: Option[String] = config("build")
  var compress: Option[String] = config("compress")
  var cache_region_size: Option[String] = config("cache_region_size")

  // Numeric args
  override def defaultThreads: Int = config("fork", default = 2)
  var cache_version: Option[Int] = config("cache_version")
  var freq_freq: Option[Float] = config("freq_freq")
  var port: Option[Int] = config("port")
  var db_version: Option[Int] = config("db_version")
  var buffer_size: Option[Int] = config("buffer_size")
  // ought to be a flag, but is BUG in VEP; becomes numeric ("1" is true)
  var failed: Option[Int] = config("failed")

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (!cache && !database) {
      Logging.addError("Must supply either cache or database for VariantEffectPredictor")
    } else if (cache && dir.isEmpty) {
      Logging.addError("Must supply dir to cache for VariantEffectPredictor")
    }
  }

  /** Returns command to execute */
  def cmdLine = required(executable) +
    required(vepScript) +
    required("-i", input) +
    required("-o", output) +
    conditional(v, "-v") +
    conditional(q, "-q") +
    conditional(offline, "--offline") +
    conditional(no_progress, "--no_progress") +
    conditional(everything, "--everything") +
    conditional(force, "--force_overwrite") +
    conditional(no_stats, "--no_stats") +
    conditional(stats_text, "--stats_text") +
    conditional(html, "--html") +
    conditional(cache, "--cache") +
    conditional(humdiv, "--humdiv") +
    conditional(regulatory, "--regulatory") +
    conditional(cell_type, "--cel_type") +
    conditional(phased, "--phased") +
    conditional(allele_number, "--allele_number") +
    conditional(numbers, "--numbers") +
    conditional(domains, "--domains") +
    conditional(no_escape, "--no_escape") +
    conditional(hgvs, "--hgvs") +
    conditional(protein, "--protein") +
    conditional(symbol, "--symbol") +
    conditional(ccds, "--ccds") +
    conditional(uniprot, "--uniprot") +
    conditional(tsl, "--tsl") +
    conditional(canonical, "--canonical") +
    conditional(biotype, "--biotype") +
    conditional(xref_refseq, "--xref_refseq") +
    conditional(check_existing, "--check_existing") +
    conditional(check_alleles, "--check_alleles") +
    conditional(check_svs, "--check_svs") +
    conditional(gmaf, "--gmaf") +
    conditional(maf_1kg, "--maf_1kg") +
    conditional(maf_esp, "--maf_esp") +
    conditional(pubmed, "--pubmed") +
    conditional(vcf, "--vcf") +
    conditional(json, "--json") +
    conditional(gvf, "--gvf") +
    conditional(check_ref, "--check_ref") +
    conditional(coding_only, "--coding_only") +
    conditional(no_intergenic, "--no_intergenic") +
    conditional(pick, "--pick") +
    conditional(pick_allele, "--pick_allele") +
    conditional(flag_pick, "--flag_pick") +
    conditional(flag_pick_allele, "--flag_pick_allele") +
    conditional(per_gene, "--per_gene") +
    conditional(most_severe, "--most_severe") +
    conditional(summary, "--summary") +
    conditional(filter_common, "--filter_common") +
    conditional(check_frequency, "--check_frequency") +
    conditional(allow_non_variant, "--allow_non_variant") +
    conditional(database, "--database") +
    conditional(genomes, "--genomes") +
    conditional(gencode_basic, "--gencode_basic") +
    conditional(refseq, "--refseq") +
    conditional(merged, "--merged") +
    conditional(all_refseq, "--all_refseq") +
    conditional(lrg, "--lrg") +
    conditional(no_whole_genome, "--no_whole_genome") +
    conditional(skip_db_check, "--skip_db_check") +
    optional("--config", vep_config) +
    optional("--species", species) +
    optional("--assembly", assembly) +
    optional("--format", format) +
    optional("--dir", dir) +
    optional("--dir_cache", dir_cache) +
    optional("--dir_plugins", dir_plugins) +
    optional("--fasta", fasta) +
    optional("--sift", sift) +
    optional("--polyphen", polyphen) +
    optional("--custom", custom) +
    repeat("--plugin", plugin) +
    optional("--individual", individual) +
    optional("--fields", fields) +
    optional("--convert", convert) +
    optional("--terms", terms) +
    optional("--chr", chr) +
    optional("--pick_order", pick_order) +
    optional("--freq_pop", freq_pop) +
    optional("--freq_gt_lt", freq_gt_lt) +
    optional("--freq_filter", freq_filter) +
    optional("--filter", filter) +
    optional("--host", host) +
    optional("--user", user) +
    optional("--password", password) +
    optional("--registry", registry) +
    optional("--build", build) +
    optional("--compress", compress) +
    optional("--cache_region_size", cache_region_size) +
    optional("--fork", threads) +
    optional("--cache_version", cache_version) +
    optional("--freq_freq", freq_freq) +
    optional("--port", port) +
    optional("--db_version", db_version) +
    optional("--buffer_size", buffer_size) +
    optional("--failed", failed)

  def summaryFiles: Map[String, File] = Map()

  def summaryStats: Map[String, Any] = {
    if (stats_text) {
      val stats_file: File = new File(output.getAbsolutePath + "_summary.txt")
      parseStatsFile(stats_file)
    } else {
      Map()
    }
  }

  def parseStatsFile(file: File): Map[String, Any] = {
    val contents = Source.fromFile(file).getLines().toList
    val headers = getHeadersFromStatsFile(contents)
    headers.foldLeft(Map.empty[String, Any])((acc, x) => acc + (x.replace(" ", "_") -> getBlockFromStatsFile(contents, x)))
  }

  def getBlockFromStatsFile(contents: List[String], header: String): Map[String, Any] = {
    var inBlock = false
    var theMap: Map[String, Any] = Map()
    for (x <- contents) {
      val stripped = x.stripPrefix("[").stripSuffix("]")
      if (stripped == header) {
        inBlock = true
      } else {
        if (inBlock) {
          val key = stripped.split('\t').head.replace(" ", "_")
          val value = stripped.split('\t').last
          theMap ++= Map(key -> tryToParseNumber(value, fallBack = true).getOrElse(value))
        }
      }
      if (stripped == "") {
        inBlock = false
      }
    }
    theMap
  }

  def getHeadersFromStatsFile(contents: List[String]): List[String] = {
    // block headers are of format '[block]'
    contents.filter(_.startsWith("[")).filter(_.endsWith("]")).map(_.stripPrefix("[")).map(_.stripSuffix("]"))
  }

}
