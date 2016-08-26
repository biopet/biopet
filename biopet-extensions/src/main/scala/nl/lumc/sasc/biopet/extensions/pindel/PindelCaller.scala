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
package nl.lumc.sasc.biopet.extensions.pindel

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Reference, Version }
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline._

/**
 * Extension for pindel
 *
 * Based on version 0.2.5b8
 */

class PindelCaller(val root: Configurable) extends BiopetCommandLineFunction with Reference with Version {
  executable = config("exe", default = "pindel")

  override def defaultCoreMemory = 4.0
  override def defaultThreads = 4

  def versionRegex = """Pindel version:? (.*)""".r
  override def versionExitcode = List(1)
  def versionCommand = executable

  /**
   * Required parameters
   */
  @Input
  var reference: File = referenceFasta

  @Input(doc = "Input specification for Pindel to use")
  var input: File = _

  @Argument(doc = "The pindel configuration file", required = false)
  var pindelFile: Option[File] = None

  @Argument(doc = "Configuration file with: bam-location/insert size/name", required = false)
  var configFile: Option[File] = None

  @Argument(doc = "Work directory")
  var outputPrefix: File = _

  @Output(doc = "Output file of Pindel, pointing to the DEL file")
  var outputFile: File = _

  @Output(doc = "", required = false)
  var outputINV: File = _
  @Output(doc = "", required = false)
  var outputTD: File = _
  @Output(doc = "", required = false)
  var outputLI: File = _
  @Output(doc = "", required = false)
  var outputBP: File = _
  @Output(doc = "", required = false)
  var outputSI: File = _
  @Output(doc = "", required = false)
  var outputRP: File = _
  @Output(doc = "", required = false)
  var outputCloseEndMapped: File = _

  var RP: Option[Int] = config("RP")
  var minDistanceToTheEnd: Option[Int] = config("min_distance_to_the_end")
  // var threads
  var maxRangeIndex: Option[Int] = config("max_range_index")
  var windowSize: Option[Int] = config("window_size")
  var sequencingErrorRate: Option[Float] = config("sequencing_error_rate")
  var sensitivity: Option[Float] = config("sensitivity")

  var maximumAllowedMismatchRate: Option[Float] = config("maximum_allowed_mismatch_rate")
  var nm: Option[Int] = config("nm")

  var reportInversions: Boolean = config("report_inversions", default = false)
  var reportDuplications: Boolean = config("report_duplications", default = false)
  var reportLongInsertions: Boolean = config("report_long_insertions", default = false)
  var reportBreakpoints: Boolean = config("report_breakpoints", default = false)
  var reportCloseMappedReads: Boolean = config("report_close_mapped_reads", default = false)
  var reportOnlyCloseMappedReads: Boolean = config("report_only_close_mapped_reads", default = false)
  var reportInterchromosomalEvents: Boolean = config("report_interchromosomal_events", default = false)

  var IndelCorrection: Boolean = config("IndelCorrection", default = false)
  var NormalSamples: Boolean = config("NormalSamples", default = false)

  var breakdancer: Option[File] = config("breakdancer")
  var include: Option[File] = config("include")
  var exclude: Option[File] = config("exclude")

  var additionalMismatch: Option[Int] = config("additional_mismatch")
  var minPerfectMatchAroundBP: Option[Int] = config("min_perfect_match_around_BP")
  var minInversionSize: Option[Int] = config("min_inversion_size")
  var minNumMatchedBases: Option[Int] = config("min_num_matched_bases")
  var balanceCutoff: Option[Int] = config("balance_cutoff")
  var anchorQuality: Option[Int] = config("anchor_quality")
  var minimumSupportForEvent: Option[Int] = config("minimum_support_for_event")
  var inputSVCallsForAssembly: Option[File] = config("input_SV_Calls_for_assembly")

  var genotyping: Boolean = config("genotyping", default = false)
  var outputOfBreakdancerEvents: Option[File] = config("output_of_breakdancer_events")
  var nameOfLogfile: Option[File] = config("name_of_logfile")

  var ploidy: Option[File] = config("ploidy")
  var detectDD: Boolean = config("detect_DD", default = false)

  var maxDdBreakpointDistance: Option[Int] = config("max_dd_breakpoint_distance")
  var maxDistanceClusterReads: Option[Int] = config("max_distance_cluster_reads")
  var minDdClusterSize: Option[Int] = config("min_dd_cluster_size")
  var minDdBreakpointSupport: Option[Int] = config("min_dd_Breakpoint_support")
  var minDdMapDistance: Option[Int] = config("min_dd_map_distance")
  var ddReportDuplicationReads: Option[Int] = config("dd_report_duplication_reads")

  override def beforeGraph: Unit = {
    if (reference == null) reference = referenceFasta()

    // we should check whether the `pindel-config-file` is set or the `config-file` for the bam-list
    // at least one of them should be set.
    (pindelFile, configFile) match {
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

    /** setting the output files for the many outputfiles pindel has */

    outputINV = new File(outputPrefix + File.separator, "sample_INV")
    outputTD = new File(outputPrefix + File.separator, "sample_TD")
    if (reportLongInsertions) {
      outputLI = new File(outputPrefix + File.separator, "sample_LI")
    }
    if (reportBreakpoints) {
      outputBP = new File(outputPrefix + File.separator, "sample_BP")
    }
    outputSI = new File(outputPrefix + File.separator, "sample_SI")

    outputRP = new File(outputPrefix + File.separator, "sample_RP")
    if (reportCloseMappedReads) {
      outputCloseEndMapped = new File(outputPrefix + File.separator, "sample_CloseEndMapped")
    }

    // set the output file, the DELetion call is always made
    outputFile = new File(outputPrefix + File.separator, "sample_D")
  }

  def cmdLine = required(executable) +
    required("--fasta ", reference) +
    optional("--pindel-config-file", pindelFile) +
    optional("--config-file", configFile) +
    required("--output-prefix ", new File(outputPrefix + File.separator, "sample")) +
    optional("--RP", RP) +
    optional("--min_distance_to_the_end", minDistanceToTheEnd) +
    optional("--number_of_threads", threads) +
    optional("--max_range_index", maxRangeIndex) +
    optional("--windows_size", windowSize) +
    optional("--sequencing_error_rate", sequencingErrorRate) +
    optional("--sensitivity", sensitivity) +
    optional("--maximum_allowed_mismatch_rate", maximumAllowedMismatchRate) +
    optional("--NM", nm) +
    conditional(reportInversions, "--report_inversions") +
    conditional(reportDuplications, "--report_duplications") +
    conditional(reportLongInsertions, "--report_long_insertions") +
    conditional(reportBreakpoints, "--report_breakpoints") +
    conditional(reportCloseMappedReads, "--report_close_mapped_reads") +
    conditional(reportOnlyCloseMappedReads, "--report_only_close_mapped_reads") +
    conditional(reportInterchromosomalEvents, "--report_interchromosomal_events") +
    conditional(IndelCorrection, "--IndelCorrection") +
    conditional(NormalSamples, "--NormalSamples") +
    optional("--breakdancer", breakdancer) +
    optional("--include", include) +
    optional("--exclude", exclude) +
    optional("--additional_mismatch", additionalMismatch) +
    optional("--min_perfect_match_around_BP", minPerfectMatchAroundBP) +
    optional("--min_inversion_size", minInversionSize) +
    optional("--min_num_matched_bases", minNumMatchedBases) +
    optional("--balance_cutoff", balanceCutoff) +
    optional("--anchor_quality", anchorQuality) +
    optional("--minimum_support_for_event", minimumSupportForEvent) +
    optional("--input_SV_Calls_for_assembly", inputSVCallsForAssembly) +
    conditional(genotyping, "-g") +
    optional("--output_of_breakdancer_events", outputOfBreakdancerEvents) +
    optional("--name_of_logfile", nameOfLogfile) +
    optional("--Ploidy", ploidy) +
    conditional(detectDD, "detect_DD") +
    optional("--MAX_DD_BREAKPOINT_DISTANCE", maxDdBreakpointDistance) +
    optional("--MAX_DISTANCE_CLUSTER_READS", maxDistanceClusterReads) +
    optional("--MIN_DD_CLUSTER_SIZE", minDdClusterSize) +
    optional("--MIN_DD_BREAKPOINT_SUPPORT", minDdBreakpointSupport) +
    optional("--MIN_DD_MAP_DISTANCE", minDdMapDistance) +
    optional("--DD_REPORT_DUPLICATION_READS", ddReportDuplicationReads)
}

object PindelCaller {
  def apply(root: Configurable, configFile: File, outputDir: File): PindelCaller = {
    val caller = new PindelCaller(root)
    caller.configFile = Some(configFile)
    caller.outputPrefix = outputDir
    caller.beforeGraph
    caller
  }
}
