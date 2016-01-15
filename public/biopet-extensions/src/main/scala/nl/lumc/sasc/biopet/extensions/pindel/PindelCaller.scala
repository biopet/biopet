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
package nl.lumc.sasc.biopet.extensions.pindel

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Reference, Version }
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input }

/**
 * Extension for pindel
 *
 * Based on version 0.2.5b8
 */

class PindelCaller(val root: Configurable) extends BiopetCommandLineFunction with Reference with Version {
  executable = config("exe", default = "pindel", freeVar = false)

  override def defaultCoreMemory = 3.0
  override def defaultThreads = 4

  override def versionRegex = """Pindel version:? (.*)""".r
  override def versionExitcode = List(1)
  override def versionCommand = executable

  /**
   * Required parameters
   */
  var reference: File = referenceFasta

  @Input(doc = "Input specification for Pindel to use")
  var input: File = _

  @Argument(doc = "The pindel configuration file")
  var pindel_file: Option[File] = None

  @Argument(doc = "Configuration file with: bam-location/insert size/name")
  var config_file: Option[File] = None

  @Argument(doc = "Work directory")
  var output_prefix: File = _

  var RP: Option[Int] = config("RP")
  var min_distance_to_the_end: Option[Int] = config("min_distance_to_the_end")
  // var threads
  var max_range_index: Option[Int] = config("max_range_index")
  var window_size: Option[Int] = config("window_size")
  var sequencing_error_rate: Option[Float] = config("sequencing_error_rate")
  var sensitivity: Option[Float] = config("sensitivity")

  var maximum_allowed_mismatch_rate: Option[Float] = config("maximum_allowed_mismatch_rate")
  var nm: Option[Int] = config("nm")

  var report_inversions: Boolean = config("report_inversions")
  var report_duplications: Boolean = config("report_duplications")
  var report_long_insertions: Boolean = config("report_long_insertions")
  var report_breakpoints: Boolean = config("report_breakpoints")
  var report_close_mapped_reads: Boolean = config("report_close_mapped_reads")
  var report_only_close_mapped_reads: Boolean = config("report_only_close_mapped_reads")
  var report_interchromosomal_events: Boolean = config("report_interchromosomal_events")

  var IndelCorrection: Boolean = config("IndelCorrection")
  var NormalSamples: Boolean = config("NormalSamples")

  var breakdancer: Option[File] = config("breakdancer")
  var include: Option[File] = config("include")
  var exclude: Option[File] = config("exclude")

  var additional_mismatch: Option[Int] = config("additional_mismatch")
  var min_perfect_match_around_BP: Option[Int] = config("min_perfect_match_around_BP")
  var min_inversion_size: Option[Int] = config("min_inversion_size")
  var min_num_matched_bases: Option[Int] = config("min_num_matched_bases")
  var balance_cutoff: Option[Int] = config("balance_cutoff")
  var anchor_quality: Option[Int] = config("anchor_quality")
  var minimum_support_for_event: Option[Int] = config("minimum_support_for_event")
  var input_SV_Calls_for_assembly: Option[File] = config("input_SV_Calls_for_assembly")

  var genotyping: Boolean = config("genotyping")
  var output_of_breakdancer_events: Option[File] = config("output_of_breakdancer_events")
  var name_of_logfile: Option[File] = config("name_of_logfile")

  var Ploidy: Option[File] = config("ploidy")
  var detect_DD: Boolean = config("detect_DD")

  var MAX_DD_BREAKPOINT_DISTANCE: Option[Int] = config("MAX_DD_BREAKPOINT_DISTANCE")
  var MAX_DISTANCE_CLUSTER_READS: Option[Int] = config("MAX_DISTANCE_CLUSTER_READS")
  var MIN_DD_CLUSTER_SIZE: Option[Int] = config("MIN_DD_CLUSTER_SIZE")
  var MIN_DD_BREAKPOINT_SUPPORT: Option[Int] = config("MIN_DD_BREAKPOINT_SUPPORT")
  var MIN_DD_MAP_DISTANCE: Option[Int] = config("MIN_DD_MAP_DISTANCE")
  var DD_REPORT_DUPLICATION_READS: Option[Int] = config("DD_REPORT_DUPLICATION_READS")

  override def beforeGraph: Unit = {
    // we should check whether the `pindel-config-file` is set or the `config-file` for the bam-list
    // at least one of them should be set.
    (pindel_file, config_file) match {
      case (None, None)       => Logging.addError("No pindel config is given")
      case (Some(a), Some(b)) => Logging.addError(s"Please specify either a pindel config or bam-config. Not both for Pindel: $a or $b")
      case (Some(a), None) => {
        Logging.logger.info(s"Using '${a}' as pindel config for Pindel")
        input = a.getAbsoluteFile
      }
      case (None, Some(b)) => {
        Logging.logger.info(s"Using '${b}' as bam config for Pindel")
        input = b.getAbsoluteFile
      }
    }
  }

  def cmdLine = required(executable) +
    required("--fasta ", reference) +
    optional("--pindel-config-file", pindel_file) +
    optional("--config-file", config_file) +
    required("--output-prefix ", output_prefix) +
    optional("--RP", RP) +
    optional("--min_distance_to_the_end", min_distance_to_the_end) +
    optional("--number_of_threads", threads) +
    optional("--max_range_index", max_range_index) +
    optional("--windows_size", window_size) +
    optional("--sequencing_error_rate", sequencing_error_rate) +
    optional("--sensitivity", sensitivity) +
    optional("--maximum_allowed_mismatch_rate", maximum_allowed_mismatch_rate) +
    optional("--NM", nm) +
    conditional(report_inversions, "--report_inversions") +
    conditional(report_duplications, "--report_duplications") +
    conditional(report_long_insertions, "--report_long_insertions") +
    conditional(report_breakpoints, "--report_breakpoints") +
    conditional(report_close_mapped_reads, "--report_close_mapped_reads") +
    conditional(report_only_close_mapped_reads, "--report_only_close_mapped_reads") +
    conditional(report_interchromosomal_events, "--report_interchromosomal_events") +
    conditional(IndelCorrection, "--IndelCorrection") +
    conditional(NormalSamples, "--NormalSamples") +
    optional("--breakdancer", breakdancer) +
    optional("--include", include) +
    optional("--exclude", exclude) +
    optional("--additional_mismatch", additional_mismatch) +
    optional("--min_perfect_match_around_BP", min_perfect_match_around_BP) +
    optional("--min_inversion_size", min_inversion_size) +
    optional("--min_num_matched_bases", min_num_matched_bases) +
    optional("--balance_cutoff", balance_cutoff) +
    optional("--anchor_quality", anchor_quality) +
    optional("--minimum_support_for_event", minimum_support_for_event) +
    optional("--input_SV_Calls_for_assembly", input_SV_Calls_for_assembly) +
    conditional(genotyping, "-g") +
    optional("--output_of_breakdancer_events", output_of_breakdancer_events) +
    optional("--name_of_logfile", name_of_logfile) +
    optional("--number_of_threads", threads) +
    optional("--Ploidy", Ploidy) +
    conditional(detect_DD, "detect_DD") +
    optional("--MAX_DD_BREAKPOINT_DISTANCE", MAX_DD_BREAKPOINT_DISTANCE) +
    optional("--MAX_DISTANCE_CLUSTER_READS", MAX_DISTANCE_CLUSTER_READS) +
    optional("--MIN_DD_CLUSTER_SIZE", MIN_DD_CLUSTER_SIZE) +
    optional("--MIN_DD_BREAKPOINT_SUPPORT", MIN_DD_BREAKPOINT_SUPPORT) +
    optional("--MIN_DD_MAP_DISTANCE", MIN_DD_MAP_DISTANCE) +
    optional("--DD_REPORT_DUPLICATION_READS", DD_REPORT_DUPLICATION_READS)
}

object PindelCaller {
  def apply(root: Configurable, configFile: File, outputDir: File): PindelCaller = {
    val caller = new PindelCaller(root)
    caller.config_file = Some(configFile)
    caller.output_prefix = outputDir
    caller
  }
}
