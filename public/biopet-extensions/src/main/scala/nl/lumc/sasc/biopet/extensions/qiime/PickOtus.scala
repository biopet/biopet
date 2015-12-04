package nl.lumc.sasc.biopet.extensions.qiime

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by pjvan_thof on 12/4/15.
 */
class PickOtus(val root: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "pick_otus.py")

  @Input(required = true)
  var inputFasta: File = _

  var outputDir: File = null

  override def defaultThreads = 2
  override def defaultCoreMemory = 5.0
  def versionCommand = executable + " --version"
  def versionRegex = """Version: (.*)""".r

  var otu_picking_method: Option[String] = config("otu_picking_method")
  var clustering_algorithm: Option[String] = config("clustering_algorithm")
  var max_cdhit_memory: Option[Int] = config("max_cdhit_memory")
  var refseqs_fp: Option[String] = config("refseqs_fp")
  var blast_db: Option[String] = config("blast_db")
  var max_e_value_blast: Option[String] = config("max_e_value_blast")
  var sortmerna_db: Option[String] = config("sortmerna_db")
  var sortmerna_e_value: Option[Double] = config("sortmerna_e_value")
  var sortmerna_coverage: Option[Double] = config("sortmerna_coverage")
  var sortmerna_tabular: Boolean = config("sortmerna_tabular", default = false)
  var sortmerna_best_N_alignments: Option[Int] = config("sortmerna_best_N_alignments")
  var sortmerna_max_pos: Option[Int] = config("sortmerna_max_pos")
  var min_aligned_percent: Option[Double] = config("min_aligned_percent")
  var similarity: Option[Double] = config("similarity")
  var sumaclust_exact: Option[String] = config("sumaclust_exact")
  var sumaclust_l: Boolean = config("sumaclust_l", default = false)
  var denovo_otu_id_prefix: Option[String] = config("denovo_otu_id_prefix")
  var swarm_resolution: Option[String] = config("swarm_resolution")
  var trie_reverse_seqs: Boolean = config("trie_reverse_seqs", default = false)
  var prefix_prefilter_length: Option[String] = config("prefix_prefilter_length")
  var trie_prefilter: Option[String] = config("trie_prefilter")
  var prefix_length: Option[String] = config("prefix_length")
  var suffix_length: Option[String] = config("suffix_length")
  var enable_rev_strand_match: Boolean = config("enable_rev_strand_match", default = false)
  var suppress_presort_by_abundance_uclust: Boolean = config("suppress_presort_by_abundance_uclust", default = false)
  var optimal_uclust: Boolean = config("optimal_uclust", default = false)
  var exact_uclust: Boolean = config("exact_uclust", default = false)
  var user_sort: Boolean = config("user_sort", default = false)
  var suppress_new_clusters: Boolean = config("suppress_new_clusters", default = false)
  var max_accepts: Option[String] = config("max_accepts")
  var max_rejects: Option[String] = config("max_rejects")
  var stepwords: Option[String] = config("stepwords")
  var word_length: Option[String] = config("word_length")
  var suppress_uclust_stable_sort: Boolean = config("suppress_uclust_stable_sort", default = false)
  var suppress_prefilter_exact_match: Boolean = config("suppress_prefilter_exact_match", default = false)
  var save_uc_files: Boolean = config("save_uc_files", default = false)
  var percent_id_err: Option[String] = config("percent_id_err")
  var minsize: Option[String] = config("minsize")
  var abundance_skew: Option[String] = config("abundance_skew")
  var db_filepath: Option[String] = config("db_filepath")
  var perc_id_blast: Option[String] = config("perc_id_blast")
  var de_novo_chimera_detection: Boolean = config("de_novo_chimera_detection", default = false)
  var suppress_de_novo_chimera_detection: Boolean = config("suppress_de_novo_chimera_detection", default = false)
  var reference_chimera_detection: Option[String] = config("reference_chimera_detection")
  var suppress_reference_chimera_detection: Option[String] = config("suppress_reference_chimera_detection")
  var cluster_size_filtering: Option[String] = config("cluster_size_filtering")
  var suppress_cluster_size_filtering: Option[String] = config("suppress_cluster_size_filtering")
  var remove_usearch_logs: Boolean = config("remove_usearch_logs", default = false)
  var derep_fullseq: Boolean = config("derep_fullseq", default = false)
  var non_chimeras_retention: Option[String] = config("non_chimeras_retention")
  var minlen: Option[String] = config("minlen")
  var usearch_fast_cluster: Boolean = config("usearch_fast_cluster", default = false)
  var usearch61_sort_method: Option[String] = config("usearch61_sort_method")
  var sizeorder: Boolean = config("sizeorder", default = false)

  private lazy val name = inputFasta.getName.stripSuffix(".fasta").stripSuffix(".fa").stripSuffix(".fna")

  def clustersFile = new File(outputDir, s"${name}_clusters.uc")
  def logFile = new File(outputDir, s"${name}_otus.log")
  def otusTxt = new File(outputDir, s"${name}_otus.txt")

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    outputFiles :+= clustersFile
    outputFiles :+= logFile
    outputFiles :+= otusTxt
  }

  def cmdLine = executable +
    required("-i", inputFasta) +
    required("-o", outputDir) +
    optional("-m", otu_picking_method) +
    optional("-c", clustering_algorithm) +
    optional("-M", max_cdhit_memory) +
    optional("-r", refseqs_fp) +
    optional("-b", blast_db) +
    optional("-e", max_e_value_blast) +
    optional("--sortmerna_db", sortmerna_db) +
    optional("--sortmerna_e_value", sortmerna_e_value) +
    optional("--sortmerna_coverage", sortmerna_coverage) +
    conditional(sortmerna_tabular, "--sortmerna_tabular") +
    optional("--sortmerna_best_N_alignments", sortmerna_best_N_alignments) +
    optional("--sortmerna_max_pos", sortmerna_max_pos) +
    optional("--min_aligned_percent", min_aligned_percent) +
    optional("--similarity", similarity) +
    optional("--sumaclust_exact", sumaclust_exact) +
    conditional(sumaclust_l, "--sumaclust_l") +
    optional("--denovo_otu_id_prefix", denovo_otu_id_prefix) +
    optional("--swarm_resolution", swarm_resolution) +
    conditional(trie_reverse_seqs, "--trie_reverse_seqs") +
    optional("--prefix_prefilter_length", prefix_prefilter_length) +
    optional("--trie_prefilter", trie_prefilter) +
    optional("--prefix_length", prefix_length) +
    optional("--suffix_length", suffix_length) +
    conditional(enable_rev_strand_match, "--enable_rev_strand_match") +
    conditional(suppress_presort_by_abundance_uclust, "--suppress_presort_by_abundance_uclust") +
    conditional(optimal_uclust, "--optimal_uclust") +
    conditional(exact_uclust, "--exact_uclust") +
    conditional(user_sort, "--user_sort") +
    conditional(suppress_new_clusters, "--suppress_new_clusters") +
    optional("--max_accepts", max_accepts) +
    optional("--max_rejects", max_rejects) +
    optional("--stepwords", stepwords) +
    optional("--word_length", word_length) +
    conditional(suppress_uclust_stable_sort, "--suppress_uclust_stable_sort") +
    conditional(suppress_prefilter_exact_match, "--suppress_prefilter_exact_match") +
    conditional(save_uc_files, "--save_uc_files") +
    optional("--percent_id_err", percent_id_err) +
    optional("--minsize", minsize) +
    optional("--abundance_skew", abundance_skew) +
    optional("--db_filepath", db_filepath) +
    optional("--perc_id_blast", perc_id_blast) +
    conditional(de_novo_chimera_detection, "--de_novo_chimera_detection") +
    conditional(suppress_de_novo_chimera_detection, "--suppress_de_novo_chimera_detection") +
    optional("--reference_chimera_detection", reference_chimera_detection) +
    optional("--suppress_reference_chimera_detection", suppress_reference_chimera_detection) +
    optional("--cluster_size_filtering", cluster_size_filtering) +
    optional("--suppress_cluster_size_filtering", suppress_cluster_size_filtering) +
    conditional(remove_usearch_logs, "--remove_usearch_logs") +
    conditional(derep_fullseq, "--derep_fullseq") +
    optional("--non_chimeras_retention", non_chimeras_retention) +
    optional("--minlen", minlen) +
    conditional(usearch_fast_cluster, "--usearch_fast_cluster") +
    optional("--usearch61_sort_method", usearch61_sort_method) +
    conditional(sizeorder, "--sizeorder") +
    optional("--threads", threads)
}
