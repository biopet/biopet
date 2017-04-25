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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.{ LazyCheck, Logging, VcfUtils, tryToParseNumber }
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Reference, Version }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.io.Source

/**
 * Extension for VariantEffectPredictor
 * Created by ahbbollen on 15-1-15.
 */
class VariantEffectPredictor(val parent: Configurable) extends BiopetCommandLineFunction with Reference with Version with Summarizable {

  lazy val vepVersion = new LazyCheck({
    val s: Option[String] = config("vep_version")
    s
  })
  vepVersion()

  executable = config("exe", namespace = "perl", default = "perl")
  var vepScript: String = config("vep_script")

  @Input(doc = "input VCF", required = true)
  var input: File = _

  @Output(doc = "output file", required = true)
  var output: File = _

  override def subPath = {
    if (vepVersion.isSet) super.subPath ++ List("vep_settings") ++ vepVersion()
    else super.subPath
  }

  def versionRegex = """version (\d*)""".r
  def versionCommand = executable + " " + vepScript + " --help"

  //Boolean vars
  var v: Boolean = config("v", default = true, freeVar = false)
  var q: Boolean = config("q", default = false, freeVar = false)
  var offline: Boolean = config("offline", default = false)
  var noProgress: Boolean = config("no_progress", default = false)
  var everything: Boolean = config("everything", default = false)
  var force: Boolean = config("force", default = false)
  var noStats: Boolean = config("no_stats", default = false)
  var statsText: Boolean = config("stats_text", default = true)
  var html: Boolean = config("html", default = false)
  var cache: Boolean = config("cache", default = false)
  var humdiv: Boolean = config("humdiv", default = false)
  var regulatory: Boolean = config("regulatory", default = false)
  var cellType: Boolean = config("cell_type", default = false)
  var phased: Boolean = config("phased", default = false)
  var alleleNumber: Boolean = config("allele_number", default = false)
  var numbers: Boolean = config("numbers", default = false)
  var domains: Boolean = config("domains", default = false)
  var noEscape: Boolean = config("no_escape", default = false)
  var hgvs: Boolean = config("hgvs", default = false)
  var protein: Boolean = config("protein", default = false)
  var symbol: Boolean = config("symbol", default = false)
  var ccds: Boolean = config("ccds", default = false)
  var uniprot: Boolean = config("uniprot", default = false)
  var tsl: Boolean = config("tsl", default = false)
  var canonical: Boolean = config("canonical", default = false)
  var biotype: Boolean = config("biotype", default = false)
  var xrefRefseq: Boolean = config("xref_refseq", default = false)
  var checkExisting: Boolean = config("check_existing", default = false)
  var checkAlleles: Boolean = config("check_alleles", default = false)
  var checkSvs: Boolean = config("svs", default = false)
  var gmaf: Boolean = config("gmaf", default = false)
  var maf1kg: Boolean = config("maf_1kg", default = false)
  var mafEsp: Boolean = config("maf_esp", default = false)
  var oldMap: Boolean = config("old_maf", default = false)
  var pubmed: Boolean = config("pubmed", default = false)

  var vcf: Boolean = config("vcf", default = true, freeVar = false)
  var json: Boolean = config("json", default = false, freeVar = false)
  var gvf: Boolean = config("gvf", default = false)
  var checkRef: Boolean = config("check_ref", default = false)
  var codingOnly: Boolean = config("coding_only", default = false)
  var noIntergenic: Boolean = config("no_intergenic", default = false)
  var pick: Boolean = config("pick", default = false)
  var pickAllele: Boolean = config("pick_allele", default = false)
  var flagPick: Boolean = config("flag_pick", default = false)
  var flagPickAllele: Boolean = config("flag_pick_allele", default = false)
  var perGene: Boolean = config("per_gene", default = false)
  var mostSevere: Boolean = config("most_severe", default = false)
  var summary: Boolean = config("summary", default = false)
  var filterCommon: Boolean = config("filter_common", default = false)
  var checkFrequency: Boolean = config("check_frequency", default = false)
  var allowNonVariant: Boolean = config("allow_non_variant", default = false)
  var database: Boolean = config("database", default = false)
  var genomes: Boolean = config("genomes", default = false)
  var gencodeBasic: Boolean = config("gencode_basic", default = false)
  var refseq: Boolean = config("refseq", default = false)
  var merged: Boolean = config("merged", default = false)
  var allRefseq: Boolean = config("all_refseq", default = false)
  var lrg: Boolean = config("lrg", default = false)
  var noWholeGenome: Boolean = config("no_whole_genome", default = false)
  var skibDbCheck: Boolean = config("skip_db_check", default = false)

  // Textual args
  var vepConfigArg: Option[String] = config("config", freeVar = false)
  var species: Option[String] = config("species", freeVar = false)
  var assembly: Option[String] = config("assembly")
  var format: Option[String] = config("format")
  var dir: Option[String] = config("dir")
  var dirCache: Option[String] = config("dir_cache")
  var dirPlugins: Option[String] = config("dir_plugins")
  var fasta: Option[String] = config("fasta")
  var sift: Option[String] = config("sift")
  var polyphen: Option[String] = config("polyphen")
  var custom: List[String] = config("custom", default = Nil)
  var plugin: List[String] = config("plugin", default = Nil)
  var individual: Option[String] = config("individual")
  var fields: Option[String] = config("fields")
  var convert: Option[String] = config("convert")
  var terms: Option[String] = config("terms")
  var chr: Option[String] = config("chr")
  var pickOrder: Option[String] = config("pick_order")
  var freqPop: Option[String] = config("check_pop")
  var freqGtLt: Option[String] = config("freq_gt_lt")
  var freqFilter: Option[String] = config("freq_filter")
  var filter: Option[String] = config("filter")
  var host: Option[String] = config("host")
  var user: Option[String] = config("user")
  var password: Option[String] = config("password")
  var registry: Option[String] = config("registry")
  var build: Option[String] = config("build")
  var compress: Option[String] = config("compress")
  var cacheRegionSize: Option[String] = config("cache_region_size")

  // Numeric args
  override def defaultThreads: Int = config("fork", default = 2)
  var cacheVersion: Option[Int] = config("cache_version")
  var freqFreq: Option[Float] = config("freq_freq")
  var port: Option[Int] = config("port")
  var dbVersion: Option[Int] = config("db_version")
  var bufferSize: Option[Int] = config("buffer_size")
  // ought to be a flag, but is BUG in VEP; becomes numeric ("1" is true)
  var failed: Option[Int] = config("failed")

  override def defaultCoreMemory = 4.0

  @Output
  private var _summary: File = _

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (!cache && !database) {
      Logging.addError("Must either set 'cache' or 'database' to true for VariantEffectPredictor")
    } else if (cache && dir.isEmpty && dirCache.isEmpty) {
      Logging.addError("Must supply 'dir_cache' to cache for VariantEffectPredictor")
    }
    if (statsText) _summary = new File(output.getAbsolutePath + "_summary.txt")
  }

  /** Returns command to execute */
  def cmdLine = {
    if (input.exists() && VcfUtils.vcfFileIsEmpty(input)) {
      val zcat = Zcat(this, input, output)
      zcat.cmdLine
    } else required(executable) +
      required(vepScript) +
      required("-i", input) +
      required("-o", output) +
      conditional(v, "-v") +
      conditional(q, "-q") +
      conditional(offline, "--offline") +
      conditional(noProgress, "--no_progress") +
      conditional(everything, "--everything") +
      conditional(force, "--force_overwrite") +
      conditional(noStats, "--no_stats") +
      conditional(statsText, "--stats_text") +
      conditional(html, "--html") +
      conditional(cache, "--cache") +
      conditional(humdiv, "--humdiv") +
      conditional(regulatory, "--regulatory") +
      conditional(cellType, "--cel_type") +
      conditional(phased, "--phased") +
      conditional(alleleNumber, "--allele_number") +
      conditional(numbers, "--numbers") +
      conditional(domains, "--domains") +
      conditional(noEscape, "--no_escape") +
      conditional(hgvs, "--hgvs") +
      conditional(protein, "--protein") +
      conditional(symbol, "--symbol") +
      conditional(ccds, "--ccds") +
      conditional(uniprot, "--uniprot") +
      conditional(tsl, "--tsl") +
      conditional(canonical, "--canonical") +
      conditional(biotype, "--biotype") +
      conditional(xrefRefseq, "--xref_refseq") +
      conditional(checkExisting, "--check_existing") +
      conditional(checkAlleles, "--check_alleles") +
      conditional(checkSvs, "--check_svs") +
      conditional(gmaf, "--gmaf") +
      conditional(maf1kg, "--maf_1kg") +
      conditional(mafEsp, "--maf_esp") +
      conditional(pubmed, "--pubmed") +
      conditional(vcf, "--vcf") +
      conditional(json, "--json") +
      conditional(gvf, "--gvf") +
      conditional(checkRef, "--check_ref") +
      conditional(codingOnly, "--coding_only") +
      conditional(noIntergenic, "--no_intergenic") +
      conditional(pick, "--pick") +
      conditional(pickAllele, "--pick_allele") +
      conditional(flagPick, "--flag_pick") +
      conditional(flagPickAllele, "--flag_pick_allele") +
      conditional(perGene, "--per_gene") +
      conditional(mostSevere, "--most_severe") +
      conditional(summary, "--summary") +
      conditional(filterCommon, "--filter_common") +
      conditional(checkFrequency, "--check_frequency") +
      conditional(allowNonVariant, "--allow_non_variant") +
      conditional(database, "--database") +
      conditional(genomes, "--genomes") +
      conditional(gencodeBasic, "--gencode_basic") +
      conditional(refseq, "--refseq") +
      conditional(merged, "--merged") +
      conditional(allRefseq, "--all_refseq") +
      conditional(lrg, "--lrg") +
      conditional(noWholeGenome, "--no_whole_genome") +
      conditional(skibDbCheck, "--skip_db_check") +
      optional("--config", vepConfigArg) +
      optional("--species", species) +
      optional("--assembly", assembly) +
      optional("--format", format) +
      optional("--dir", dir) +
      optional("--dir_cache", dirCache) +
      optional("--dir_plugins", dirPlugins) +
      optional("--fasta", fasta) +
      optional("--sift", sift) +
      optional("--polyphen", polyphen) +
      repeat("--custom", custom) +
      repeat("--plugin", plugin) +
      optional("--individual", individual) +
      optional("--fields", fields) +
      optional("--convert", convert) +
      optional("--terms", terms) +
      optional("--chr", chr) +
      optional("--pick_order", pickOrder) +
      optional("--freq_pop", freqPop) +
      optional("--freq_gt_lt", freqGtLt) +
      optional("--freq_filter", freqFilter) +
      optional("--filter", filter) +
      optional("--host", host) +
      optional("--user", user) +
      optional("--password", password) +
      optional("--registry", registry) +
      optional("--build", build) +
      optional("--compress", compress) +
      optional("--cache_region_size", cacheRegionSize) +
      optional("--fork", threads) +
      optional("--cache_version", cacheVersion) +
      optional("--freq_freq", freqFreq) +
      optional("--port", port) +
      optional("--db_version", dbVersion) +
      optional("--buffer_size", bufferSize) +
      optional("--failed", failed)
  }

  def summaryFiles: Map[String, File] = Map()

  def summaryStats: Map[String, Any] = {
    val statsFile = new File(output.getAbsolutePath + "_summary.txt")
    if (statsText && statsFile.exists()) parseStatsFile(statsFile)
    else Map()
  }

  protected val removeOnConflict = Set("Output_file", "Run_time", "Start_time", "End_time", "Novel_/_existing_variants", "Input_file_(format)")
  protected val nonNumber = Set("VEP_version_(API)", "Cache/Database", "Species", "Command_line_options")

  override def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    if (removeOnConflict.contains(key)) None
    else if (nonNumber.contains(key)) v2
    else {
      (v1, v2) match {
        case (x1: Int, x2: Int) => x1 + x2
        case _                  => throw new IllegalStateException(s"Value are not Int's, unable to sum them up, key: $key, v1: $v1, v2: $v2")
      }
    }
  }

  def parseStatsFile(file: File): Map[String, Any] = {
    val reader = Source.fromFile(file)
    val contents = reader.getLines().filter(_ != "").toArray
    reader.close()

    def isHeader(line: String) = line.startsWith("[") && line.endsWith("]")

    val headers = contents.zipWithIndex
      .filter(x => x._1.startsWith("[") && x._1.endsWith("]"))

    (for ((header, headerIndex) <- headers) yield {
      val name = header.stripPrefix("[").stripSuffix("]")
      name.replaceAll(" ", "_") -> contents.drop(headerIndex + 1).takeWhile(!isHeader(_)).flatMap { line =>
        val values = line.split("\t", 2)
        if (values.last.isEmpty || values.last == "-") None
        else Some(values.head.replaceAll(" ", "_") -> tryToParseNumber(values.last).getOrElse(values.last))
      }.toMap
    }).toMap
  }
}
