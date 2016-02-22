package nl.lumc.sasc.biopet.extensions.qiime

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by pjvan_thof on 12/4/15.
 */
class AssignTaxonomy(val root: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "assign_taxonomy.py")

  @Input(required = true)
  var inputFasta: File = _

  @Input(required = false)
  var read_1_seqs_fp: Option[File] = None

  @Input(required = false)
  var read_2_seqs_fp: Option[File] = None

  @Input(required = false)
  var id_to_taxonomy_fp: Option[File] = config("id_to_taxonomy_fp")

  @Input(required = false)
  var reference_seqs_fp: Option[File] = config("reference_seqs_fp")

  @Input(required = false)
  var training_data_properties_fp: Option[File] = config("training_data_properties_fp")

  var single_ok: Boolean = config("single_ok", default = false)
  var no_single_ok_generic: Boolean = config("no_single_ok_generic", default = false)

  var amplicon_id_regex: Option[String] = config("amplicon_id_regex")
  var header_id_regex: Option[String] = config("header_id_regex")
  var assignment_method: Option[String] = config("assignment_method")
  var sortmerna_db: Option[String] = config("sortmerna_db")
  var sortmerna_e_value: Option[String] = config("sortmerna_e_value")
  var sortmerna_coverage: Option[String] = config("sortmerna_coverage")
  var sortmerna_best_N_alignments: Option[String] = config("sortmerna_best_N_alignments")
  var sortmerna_threads: Option[String] = config("sortmerna_threads")
  var blast_db: Option[String] = config("blast_db")
  var confidence: Option[String] = config("confidence")
  var min_consensus_fraction: Option[String] = config("min_consensus_fraction")
  var similarity: Option[String] = config("similarity")
  var uclust_max_accepts: Option[String] = config("uclust_max_accepts")
  var rdp_max_memory: Option[String] = config("rdp_max_memory")
  var blast_e_value: Option[String] = config("blast_e_value")
  var outputDir: File = _

  def versionCommand = executable + " --version"
  def versionRegex = """Version: (.*)""".r
  override def defaultCoreMemory = 4.0

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(outputDir != null)
  }

  def cmdLine = executable +
    required("-i", inputFasta) +
    optional("--read_1_seqs_fp", read_1_seqs_fp) +
    optional("--read_2_seqs_fp", read_2_seqs_fp) +
    optional("-t", id_to_taxonomy_fp) +
    optional("-r", reference_seqs_fp) +
    optional("-p", training_data_properties_fp) +
    optional("--amplicon_id_regex", amplicon_id_regex) +
    optional("--header_id_regex", header_id_regex) +
    optional("--assignment_method", assignment_method) +
    optional("--sortmerna_db", sortmerna_db) +
    optional("--sortmerna_e_value", sortmerna_e_value) +
    optional("--sortmerna_coverage", sortmerna_coverage) +
    optional("--sortmerna_best_N_alignments", sortmerna_best_N_alignments) +
    optional("--sortmerna_threads", sortmerna_threads) +
    optional("--blast_db", blast_db) +
    optional("--confidence", confidence) +
    optional("--min_consensus_fraction", min_consensus_fraction) +
    optional("--similarity", similarity) +
    optional("--uclust_max_accepts", uclust_max_accepts) +
    optional("--rdp_max_memory", rdp_max_memory) +
    optional("--blast_e_value", blast_e_value) +
    required("--output_dir", outputDir) +
    conditional(single_ok, "--single_ok") +
    conditional(no_single_ok_generic, "--no_single_ok_generic")
}
