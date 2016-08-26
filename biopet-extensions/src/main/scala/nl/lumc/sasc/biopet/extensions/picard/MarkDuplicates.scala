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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.summary.Summarizable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/** Extension for picard MarkDuplicates */
class MarkDuplicates(val root: Configurable) extends Picard with Summarizable {

  javaMainClass = new picard.sam.markduplicates.MarkDuplicates().getClass.getName

  @Input(doc = "The input SAM or BAM files to analyze.  Must be coordinate sorted.", required = true)
  var input: List[File] = Nil

  @Output(doc = "The output file to bam file to", required = true)
  var output: File = _

  @Output(doc = "The output file to write statistics to", required = true)
  var outputMetrics: File = _

  @Argument(doc = "PROGRAM_RECORD_ID", required = false)
  var programRecordId: Option[String] = config("programrecordid")

  @Argument(doc = "PROGRAM_GROUP_VERSION", required = false)
  var programGroupVersion: Option[String] = config("programgroupversion")

  @Argument(doc = "PROGRAM_GROUP_COMMAND_LINE", required = false)
  var programGroupCommandLine: Option[String] = config("programgroupcommandline")

  @Argument(doc = "PROGRAM_GROUP_NAME", required = false)
  var programGroupName: Option[String] = config("programgroupname")

  @Argument(doc = "COMMENT", required = false)
  var comment: Option[String] = config("comment")

  @Argument(doc = "REMOVE_DUPLICATES", required = false)
  var removeDuplicates: Boolean = config("removeduplicates", default = false)

  @Argument(doc = "ASSUME_SORTED", required = false)
  var assumeSorted: Boolean = config("assumesorted", default = false)

  @Argument(doc = "MAX_SEQUENCES_FOR_DISK_READ_ENDS_MAP", required = false)
  var maxSequencesForDiskReadEndsMap: Option[Int] = config("maxSequencesForDiskReadEndsMap")

  @Argument(doc = "MAX_FILE_HANDLES_FOR_READ_ENDS_MAP", required = false)
  var maxFileHandlesForReadEndsMap: Option[Int] = config("maxFileHandlesForReadEndsMap")

  @Argument(doc = "SORTING_COLLECTION_SIZE_RATIO", required = false)
  var sortingCollectionSizeRatio: Option[Double] = config("sortingCollectionSizeRatio")

  @Argument(doc = "READ_NAME_REGEX", required = false)
  var readNameRegex: Option[String] = config("readNameRegex")

  @Argument(doc = "OPTICAL_DUPLICATE_PIXEL_DISTANCE", required = false)
  var opticalDuplicatePixelDistance: Option[Int] = config("opticalDuplicatePixelDistance")

  @Output(doc = "Bam Index", required = true)
  private var outputIndex: File = _

  override def beforeGraph() {
    super.beforeGraph()
    if (createIndex) outputIndex = new File(output.getAbsolutePath.stripSuffix(".bam") + ".bai")
  }

  /** Returns command to execute */
  override def cmdLine = super.cmdLine +
    repeat("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    required("METRICS_FILE=", outputMetrics, spaceSeparated = false) +
    optional("PROGRAM_RECORD_ID=", programRecordId, spaceSeparated = false) +
    optional("PROGRAM_GROUP_VERSION=", programGroupVersion, spaceSeparated = false) +
    optional("PROGRAM_GROUP_COMMAND_LINE=", programGroupCommandLine, spaceSeparated = false) +
    optional("PROGRAM_GROUP_NAME=", programGroupName, spaceSeparated = false) +
    optional("COMMENT=", comment, spaceSeparated = false) +
    conditional(removeDuplicates, "REMOVE_DUPLICATES=TRUE") +
    conditional(assumeSorted, "ASSUME_SORTED=TRUE") +
    optional("MAX_SEQUENCES_FOR_DISK_READ_ENDS_MAP=", maxSequencesForDiskReadEndsMap, spaceSeparated = false) +
    optional("MAX_FILE_HANDLES_FOR_READ_ENDS_MAP=", maxFileHandlesForReadEndsMap, spaceSeparated = false) +
    optional("SORTING_COLLECTION_SIZE_RATIO=", sortingCollectionSizeRatio, spaceSeparated = false) +
    optional("READ_NAME_REGEX=", readNameRegex, spaceSeparated = false) +
    optional("OPTICAL_DUPLICATE_PIXEL_DISTANCE=", opticalDuplicatePixelDistance, spaceSeparated = false)

  /** Returns files for summary */
  def summaryFiles: Map[String, File] = Map()

  /** Returns stats for summary */
  def summaryStats = Picard.getMetrics(outputMetrics).getOrElse(Map())
}
object MarkDuplicates {
  /** Returns default MarkDuplicates */
  def apply(root: Configurable, input: List[File], output: File, isIntermediate: Boolean = false): MarkDuplicates = {
    val markDuplicates = new MarkDuplicates(root)
    markDuplicates.input = input
    markDuplicates.output = output
    markDuplicates.outputMetrics = new File(output.getParent, output.getName.stripSuffix(".bam") + ".metrics")
    markDuplicates.isIntermediate = isIntermediate
    markDuplicates
  }
}