package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

class MarkDuplicates(val root: Configurable) extends Picard {
  javaMainClass = "picard.sam.MarkDuplicates"

  @Input(doc = "The input SAM or BAM files to analyze.  Must be coordinate sorted.", required = true)
  var input: List[File] = Nil

  @Output(doc = "The output file to bam file to", required = true)
  var output: File = _

  @Output(doc = "The output file to write statistics to", required = true)
  var outputMetrics: File = _

  @Argument(doc = "PROGRAM_RECORD_ID", required = false)
  var programRecordId: String = config("programrecordid")

  @Argument(doc = "PROGRAM_GROUP_VERSION", required = false)
  var programGroupVersion: String = config("programgroupversion")

  @Argument(doc = "PROGRAM_GROUP_COMMAND_LINE", required = false)
  var programGroupCommandLine: String = config("programgroupcommandline")

  @Argument(doc = "PROGRAM_GROUP_NAME", required = false)
  var programGroupName: String = config("programgroupname")

  @Argument(doc = "COMMENT", required = false)
  var comment: String = config("comment")

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
  var readNameRegex: String = config("readNameRegex")

  @Argument(doc = "OPTICAL_DUPLICATE_PIXEL_DISTANCE", required = false)
  var opticalDuplicatePixelDistance: Option[Int] = config("opticalDuplicatePixelDistance")

  @Output(doc = "Bam Index", required = true)
  private var outputIndex: File = _
  
  override def afterGraph {
    super.afterGraph
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