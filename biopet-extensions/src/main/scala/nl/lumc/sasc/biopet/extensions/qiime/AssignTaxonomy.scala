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

import nl.lumc.sasc.biopet.core.{Version, BiopetCommandLineFunction}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
  * Created by pjvan_thof on 12/4/15.
  */
class AssignTaxonomy(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "assign_taxonomy.py")

  @Input(required = true)
  var inputFasta: File = _

  @Input(required = false)
  var read1SeqsFp: Option[File] = None

  @Input(required = false)
  var read2SeqsFp: Option[File] = None

  @Input(required = false)
  var idToTaxonomyFp: Option[File] = config("id_to_taxonomy_fp")

  @Input(required = false)
  var referenceSeqsFp: Option[File] = config("reference_seqs_fp")

  @Input(required = false)
  var trainingDataPropertiesFp: Option[File] = config("training_data_properties_fp")

  var singleOk: Boolean = config("single_ok", default = false)
  var noSingleOkGeneric: Boolean = config("no_single_ok_generic", default = false)

  var ampliconIdRegex: Option[String] = config("amplicon_id_regex")
  var headerIdRegex: Option[String] = config("header_id_regex")
  var assignmentMethod: Option[String] = config("assignment_method")
  var sortmernaDb: Option[String] = config("sortmerna_db")
  var sortmernaEValue: Option[String] = config("sortmerna_e_value")
  var sortmernaCoverage: Option[String] = config("sortmerna_coverage")
  var sortmernaBestNAlignments: Option[String] = config("sortmerna_best_N_alignments")
  var sortmernaThreads: Option[String] = config("sortmerna_threads")
  var blastDb: Option[String] = config("blast_db")
  var confidence: Option[String] = config("confidence")
  var minConsensusFraction: Option[String] = config("min_consensus_fraction")
  var similarity: Option[String] = config("similarity")
  var uclustMaxAccepts: Option[String] = config("uclust_max_accepts")
  var rdpMaxMemory: Option[String] = config("rdp_max_memory")
  var blastEValue: Option[String] = config("blast_e_value")
  var outputDir: File = _

  def versionCommand = executable + " --version"
  def versionRegex = """Version: (.*)""".r
  override def defaultCoreMemory = 4.0

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(outputDir != null)
  }

  def cmdLine =
    executable +
      required("-i", inputFasta) +
      optional("--read_1_seqs_fp", read1SeqsFp) +
      optional("--read_2_seqs_fp", read2SeqsFp) +
      optional("-t", idToTaxonomyFp) +
      optional("-r", referenceSeqsFp) +
      optional("-p", trainingDataPropertiesFp) +
      optional("--amplicon_id_regex", ampliconIdRegex) +
      optional("--header_id_regex", headerIdRegex) +
      optional("--assignment_method", assignmentMethod) +
      optional("--sortmerna_db", sortmernaDb) +
      optional("--sortmerna_e_value", sortmernaEValue) +
      optional("--sortmerna_coverage", sortmernaCoverage) +
      optional("--sortmerna_best_N_alignments", sortmernaBestNAlignments) +
      optional("--sortmerna_threads", sortmernaThreads) +
      optional("--blast_db", blastDb) +
      optional("--confidence", confidence) +
      optional("--min_consensus_fraction", minConsensusFraction) +
      optional("--similarity", similarity) +
      optional("--uclust_max_accepts", uclustMaxAccepts) +
      optional("--rdp_max_memory", rdpMaxMemory) +
      optional("--blast_e_value", blastEValue) +
      required("--output_dir", outputDir) +
      conditional(singleOk, "--single_ok") +
      conditional(noSingleOkGeneric, "--no_single_ok_generic")
}
