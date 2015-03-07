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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.Summarizable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

class MarkDuplicates(val root: Configurable) extends Picard with Summarizable {
  javaMainClass = "picard.sam.MarkDuplicates"

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

  override def beforeGraph {
    super.beforeGraph
    if (createIndex) outputIndex = new File(output.getAbsolutePath.stripSuffix(".bam") + ".bai")
  }

  override def commandLine = super.commandLine +
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

  def summaryFiles: Map[String, File] = Map()

  def summaryStats: Map[String, Any] = Picard.getMetrics(outputMetrics) match {
    case None => Map()
    case Some((header, content)) =>
      (for (category <- 0 until content.size) yield {
        content(category)(0) -> (
          for (
            i <- 1 until header.size if i < content(category).size
          ) yield {
            header(i).toLowerCase -> content(category)(i)
          }).toMap
      }
      ).toMap
  }
}
object MarkDuplicates {
  def apply(root: Configurable, input: List[File], outputDir: String): MarkDuplicates = {
    val markDuplicates = new MarkDuplicates(root)
    markDuplicates.input = input
    markDuplicates.output = new File(outputDir, input.head.getName.stripSuffix(".bam") + ".dedup.bam")
    markDuplicates.outputMetrics = new File(outputDir, input.head.getName.stripSuffix(".bam") + ".dedup.metrics")
    return markDuplicates
  }

  def apply(root: Configurable, input: List[File], output: File): MarkDuplicates = {
    val markDuplicates = new MarkDuplicates(root)
    markDuplicates.input = input
    markDuplicates.output = output
    markDuplicates.outputMetrics = new File(output.getParent, output.getName.stripSuffix(".bam") + ".metrics")
    return markDuplicates
  }
}