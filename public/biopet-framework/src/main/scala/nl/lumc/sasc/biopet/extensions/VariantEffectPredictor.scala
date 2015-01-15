package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by ahbbollen on 15-1-15.
 */
class VariantEffectPredictor(val root: Configurable) extends BiopetCommandLineFunction {

  executable = config("exe", submodule = "perl", default = "perl")
  var vep_script: String = config("vep_script", required = true)

  @Input(doc = "input VCF", required = true)
  var input: File = _

  @Output(doc = "output file", required = true)
  var output: File = _

  override val versionRegex = """version (\d*)""".r
  override def versionCommand = executable + " " + vep_script + " --help"

  //Boolean vars
  var v: Boolean = config("v")
  var q: Boolean = config("q")
  var no_progress: Boolean = config("no_progress")
  var everything: Boolean = config("everything")
  var force: Boolean = config("force")
  var no_stats: Boolean = config("no_stats")
  var stats_text: Boolean = config("stats_text")
  var html: Boolean = config("html")
  var cache: Boolean = config("cache")
  var humdiv: Boolean = config("humdiv")
  var regulatory: Boolean = config("regulatory")
  var cell_type: Boolean = config("cell_type")
  var phased: Boolean = config("phased")
  var allele_number: Boolean = config("allele_number")
  var numbers: Boolean = config("numbers")
  var domains: Boolean = config("domains")
  var no_escape: Boolean = config("no_escape")
  var hgvs: Boolean = config("hgvs")
  var protein: Boolean = config("protein")
  var symbol: Boolean = config("symbol")
  var ccds: Boolean = config("ccds")
  var uniprot: Boolean = config("uniprot")
  var tsl: Boolean = config("tsl")
  var canonical: Boolean = config("canonical")
  var biotype: Boolean = config("biotype")
  var xref_refseq: Boolean = config("xref_refseq")
  var check_existing: Boolean = config("check_existing")
  var check_alleles: Boolean = config("check_alleles")
  var check_svs: Boolean = config("svs")
  var gmaf: Boolean = config("gmaf")
  var maf_1kg: Boolean = config("maf_1kg")
  var maf_esp: Boolean = config("maf_esp")
  var old_map: Boolean = config("old_maf")
  var pubmed: Boolean = config("pubmed")
  var failed: Boolean = config("failed")
  var vcf: Boolean = config("vcf", default = true)
  var json: Boolean = config("json")
  var gvf: Boolean = config("gvf")
  var check_ref: Boolean = config("check_ref")
  var coding_only: Boolean = config("coding_only")
  var no_intergenic: Boolean = config("no_intergenic")
  var pick: Boolean = config("pick")
  var pick_allele: Boolean = config("pick_allele")
  var flag_pick: Boolean = config("flag_pick")
  var flag_pick_allele: Boolean = config("flag_pick_allele")
  var per_gene: Boolean = config("per_gene")
  var most_severe: Boolean = config("most_severe")
  var summary: Boolean = config("summary")
  var filter_common: Boolean = config("filter_common")
  var check_frequency: Boolean = config("check_frequency")
  var allow_non_variant: Boolean = config("allow_non_variant")
  var database: Boolean = config("database")
  var genomes: Boolean = config("genomes")
  var gencode_basic: Boolean = config("gencode_basic")
  var refseq: Boolean = config("refseq")
  var merged: Boolean = config("merged")
  var all_refseq: Boolean = config("all_refseq")
  var lrg: Boolean = config("lrg")
  var no_whole_genome: Boolean = config("no_whole_genome")
  var skip_db_check: Boolean = config("skip_db_check")

  // Textual args
  var vep_config: String = config("config")
  var species: String = config("species")
  var assembly: String = config("assembly")
  var format: String = config("format")
  var dir: String = config("dir")
  var dir_cache: String = config("dir_cache")
  var dir_plugins: String = config("dir_plugins")
  var fasta: File = config("fasta")
  var sift: String = config("sift")
  var polyphen: String = config("polyphen")
  var custom: String = config("custom")
  var plugin: List[String] = config("plugin")
  var individual: String = config("individual")
  var fields: String = config("fields")
  var convert: String = config("convert")
  var terms: String = config("terms")
  var chr: String = config("chr")
  var pick_order: String = config("pick_order")
  var freq_pop: String = config("check_pop")
  var freq_gt_lt: String = config("freq_gt_lt")
  var freq_filter: String = config("freq_filter")
  var filter: String = config("filter")
  var host: String = config("host")
  var user: String = config("user")
  var password: String = config("password")
  var registry: String = config("registry")
  var build: String = config("build")
  var compress: String = config("compress")
  var cache_region_size: String = config("cache_region_size")

  // Numeric args
  var fork: Option[Int] = config("fork")
  var cache_version: Option[Int] = config("cache_version")
  var freq_freq: Option[Float] = config("freq_freq")
  var port: Option[Int] = config("port")
  var db_version: Option[Int] = config("db_version")
  var buffer_size: Option[Int] = config("buffer_size")

  def cmdLine = required(executable) +
    required(vep_script) +
    required("-i", input) +
    required("-o", output) +
    conditional(v, "-v") +
    conditional(q, "-q") +
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
    conditional(failed, "--failed") +
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
    optional("--fork", fork) +
    optional("--cache_version", cache_version) +
    optional("--freq_freq", freq_freq) +
    optional("--port", port) +
    optional("--db_version", db_version) +
    optional("--buffer_size", buffer_size)

}
