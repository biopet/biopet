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
package nl.lumc.sasc.biopet.extensions.qiime

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by pjvan_thof on 12/4/15.
  */
class PickOtus(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "pick_otus.py")

  @Input(required = true)
  var inputFasta: File = _

  var outputDir: File = null

  override def defaultThreads = 2
  override def defaultCoreMemory = 5.0
  def versionCommand = executable + " --version"
  def versionRegex = """Version: (.*)""".r

  var otuPickingMethod: Option[String] = config("otu_picking_method")
  var clusteringAlgorithm: Option[String] = config("clustering_algorithm")
  var maxCdhitMemory: Option[Int] = config("max_cdhit_memory")
  var refseqsFp: Option[String] = config("refseqs_fp")
  var blastDb: Option[String] = config("blast_db")
  var maxEValueBlast: Option[String] = config("max_e_value_blast")
  var sortmernaDb: Option[String] = config("sortmerna_db")
  var sortmernaEValue: Option[Double] = config("sortmerna_e_value")
  var sortmernaCoverage: Option[Double] = config("sortmerna_coverage")
  var sortmernaTabular: Boolean = config("sortmerna_tabular", default = false)
  var sortmernaBestNAlignments: Option[Int] = config("sortmerna_best_N_alignments")
  var sortmernaMaxPos: Option[Int] = config("sortmerna_max_pos")
  var minAlignedPercent: Option[Double] = config("min_aligned_percent")
  var similarity: Option[Double] = config("similarity")
  var sumaclustExact: Option[String] = config("sumaclust_exact")
  var sumaclustL: Boolean = config("sumaclust_l", default = false)
  var denovoOtuIdPrefix: Option[String] = config("denovo_otu_id_prefix")
  var swarmResolution: Option[String] = config("swarm_resolution")
  var trieReverseSeqs: Boolean = config("trie_reverse_seqs", default = false)
  var prefixPrefilterLength: Option[String] = config("prefix_prefilter_length")
  var triePrefilter: Option[String] = config("trie_prefilter")
  var prefixLength: Option[String] = config("prefix_length")
  var suffixLength: Option[String] = config("suffix_length")
  var enableRevStrandMatch: Boolean = config("enable_rev_strand_match", default = false)
  var suppressPresortByAbundanceUclust: Boolean =
    config("suppress_presort_by_abundance_uclust", default = false)
  var optimalUclust: Boolean = config("optimal_uclust", default = false)
  var exactUclust: Boolean = config("exact_uclust", default = false)
  var userSort: Boolean = config("user_sort", default = false)
  var suppressNewClusters: Boolean = config("suppress_new_clusters", default = false)
  var maxAccepts: Option[String] = config("max_accepts")
  var maxRejects: Option[String] = config("max_rejects")
  var stepwords: Option[String] = config("stepwords")
  var wordLength: Option[String] = config("word_length")
  var suppressUclustStableSort: Boolean = config("suppress_uclust_stable_sort", default = false)
  var suppressPrefilterExactMatch: Boolean =
    config("suppress_prefilter_exact_match", default = false)
  var saveUcFiles: Boolean = config("save_uc_files", default = false)
  var percentIdErr: Option[String] = config("percent_id_err")
  var minSize: Option[String] = config("minsize")
  var abundanceSkew: Option[String] = config("abundance_skew")
  var dbFilepath: Option[String] = config("db_filepath")
  var percIdBlast: Option[String] = config("perc_id_blast")
  var deNovoChimeraDetection: Boolean = config("de_novo_chimera_detection", default = false)
  var suppressDeNovoChimeraDetection: Boolean =
    config("suppress_de_novo_chimera_detection", default = false)
  var referenceChimeraDetection: Option[String] = config("reference_chimera_detection")
  var suppressReferenceChimeraDetection: Option[String] = config(
    "suppress_reference_chimera_detection")
  var clusterSizeFiltering: Option[String] = config("cluster_size_filtering")
  var suppressClusterSizeFiltering: Option[String] = config("suppress_cluster_size_filtering")
  var removeUsearchLogs: Boolean = config("remove_usearch_logs", default = false)
  var derepFullseq: Boolean = config("derep_fullseq", default = false)
  var nonChimerasRetention: Option[String] = config("non_chimeras_retention")
  var minlen: Option[String] = config("minlen")
  var usearchFastCluster: Boolean = config("usearch_fast_cluster", default = false)
  var usearch61SortMethod: Option[String] = config("usearch61_sort_method")
  var sizeorder: Boolean = config("sizeorder", default = false)

  private lazy val name =
    inputFasta.getName.stripSuffix(".fasta").stripSuffix(".fa").stripSuffix(".fna")

  def clustersFile = new File(outputDir, s"${name}_clusters.uc")
  def logFile = new File(outputDir, s"${name}_otus.log")
  def otusTxt = new File(outputDir, s"${name}_otus.txt")

  @Output
  private var outputFiles: List[File] = Nil

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    outputFiles :+= clustersFile
    outputFiles :+= logFile
    outputFiles :+= otusTxt
  }

  def cmdLine =
    executable +
      required("-i", inputFasta) +
      required("-o", outputDir) +
      optional("-m", otuPickingMethod) +
      optional("-c", clusteringAlgorithm) +
      optional("-M", maxCdhitMemory) +
      optional("-r", refseqsFp) +
      optional("-b", blastDb) +
      optional("-e", maxEValueBlast) +
      optional("--sortmerna_db", sortmernaDb) +
      optional("--sortmerna_e_value", sortmernaEValue) +
      optional("--sortmerna_coverage", sortmernaCoverage) +
      conditional(sortmernaTabular, "--sortmerna_tabular") +
      optional("--sortmerna_best_N_alignments", sortmernaBestNAlignments) +
      optional("--sortmerna_max_pos", sortmernaMaxPos) +
      optional("--min_aligned_percent", minAlignedPercent) +
      optional("--similarity", similarity) +
      optional("--sumaclust_exact", sumaclustExact) +
      conditional(sumaclustL, "--sumaclust_l") +
      optional("--denovo_otu_id_prefix", denovoOtuIdPrefix) +
      optional("--swarm_resolution", swarmResolution) +
      conditional(trieReverseSeqs, "--trie_reverse_seqs") +
      optional("--prefix_prefilter_length", prefixPrefilterLength) +
      optional("--trie_prefilter", triePrefilter) +
      optional("--prefix_length", prefixLength) +
      optional("--suffix_length", suffixLength) +
      conditional(enableRevStrandMatch, "--enable_rev_strand_match") +
      conditional(suppressPresortByAbundanceUclust, "--suppress_presort_by_abundance_uclust") +
      conditional(optimalUclust, "--optimal_uclust") +
      conditional(exactUclust, "--exact_uclust") +
      conditional(userSort, "--user_sort") +
      conditional(suppressNewClusters, "--suppress_new_clusters") +
      optional("--max_accepts", maxAccepts) +
      optional("--max_rejects", maxRejects) +
      optional("--stepwords", stepwords) +
      optional("--word_length", wordLength) +
      conditional(suppressUclustStableSort, "--suppress_uclust_stable_sort") +
      conditional(suppressPrefilterExactMatch, "--suppress_prefilter_exact_match") +
      conditional(saveUcFiles, "--save_uc_files") +
      optional("--percent_id_err", percentIdErr) +
      optional("--minsize", minSize) +
      optional("--abundance_skew", abundanceSkew) +
      optional("--db_filepath", dbFilepath) +
      optional("--perc_id_blast", percIdBlast) +
      conditional(deNovoChimeraDetection, "--de_novo_chimera_detection") +
      conditional(suppressDeNovoChimeraDetection, "--suppress_de_novo_chimera_detection") +
      optional("--reference_chimera_detection", referenceChimeraDetection) +
      optional("--suppress_reference_chimera_detection", suppressReferenceChimeraDetection) +
      optional("--cluster_size_filtering", clusterSizeFiltering) +
      optional("--suppress_cluster_size_filtering", suppressClusterSizeFiltering) +
      conditional(removeUsearchLogs, "--remove_usearch_logs") +
      conditional(derepFullseq, "--derep_fullseq") +
      optional("--non_chimeras_retention", nonChimerasRetention) +
      optional("--minlen", minlen) +
      conditional(usearchFastCluster, "--usearch_fast_cluster") +
      optional("--usearch61_sort_method", usearch61SortMethod) +
      conditional(sizeorder, "--sizeorder") +
      optional("--threads", threads)
}
