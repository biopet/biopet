package nl.lumc.sasc.biopet.function.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output, Argument}

class MarkDuplicates(val root:Configurable) extends Picard {
  javaMainClass = "net.sf.picard.sam.MarkDuplicates"

  @Input(doc="The input SAM or BAM files to analyze.  Must be coordinate sorted.", required = true)
  var input: List[File] = Nil

  @Output(doc="The output file to bam file to", required = true)
  var output: File = _
  
  @Output(doc="The output file to write statistics to", required = true)
  var outputMetrics: File = _
  
  @Argument(doc="PROGRAM_RECORD_ID", required=false)
  var programRecordId: String = if (configContains("programrecordid")) config("programrecordid") else null
  
  @Argument(doc="PROGRAM_GROUP_VERSION", required=false)
  var programGroupVersion: String = if (configContains("programgroupversion")) config("programgroupversion") else null
  
  @Argument(doc="PROGRAM_GROUP_COMMAND_LINE", required=false)
  var programGroupCommandLine: String = if (configContains("programgroupcommandline")) config("programgroupcommandline") else null
  
  @Argument(doc="PROGRAM_GROUP_NAME", required=false)
  var programGroupName: String = if (configContains("programgroupname")) config("programgroupname") else null
  
  @Argument(doc="COMMENT", required=false)
  var comment: String = if (configContains("comment")) config("comment") else null
  
  @Argument(doc="REMOVE_DUPLICATES", required=false)
  var removeDuplicates: Boolean = config("removeduplicates", false)
  
  @Argument(doc="ASSUME_SORTED", required=false)
  var assumeSorted: Boolean = config("assumesorted", false)
  
  @Argument(doc="MAX_SEQUENCES_FOR_DISK_READ_ENDS_MAP", required=false)
  var maxSequencesForDiskReadEndsMap: Int = config("maxSequencesForDiskReadEndsMap", 50000)
  
  @Argument(doc="MAX_FILE_HANDLES_FOR_READ_ENDS_MAP", required=false)
  var maxFileHandlesForReadEndsMap: Int = config("maxFileHandlesForReadEndsMap", 8000)
  
  @Argument(doc="SORTING_COLLECTION_SIZE_RATIO", required=false)
  var sortingCollectionSizeRatio: Double = config("sortingCollectionSizeRatio", 0.25)
  
  @Argument(doc="READ_NAME_REGEX", required=false)
  var readNameRegex: String = if (configContains("readNameRegex")) config("readNameRegex") else null
  
  @Argument(doc="OPTICAL_DUPLICATE_PIXEL_DISTANCE", required=false)
  var opticalDuplicatePixelDistance: Int = config("opticalDuplicatePixelDistance", 100)
  
  override def commandLine = super.commandLine +
    repeat("INPUT=", input, spaceSeparated=false) +
    required("OUTPUT=", output, spaceSeparated=false) +
    required("METRICS_FILE=", outputMetrics, spaceSeparated=false) +
    optional("PROGRAM_RECORD_ID=", programRecordId, spaceSeparated=false) + 
    optional("PROGRAM_GROUP_VERSION=", programGroupVersion, spaceSeparated=false) +
    optional("PROGRAM_GROUP_COMMAND_LINE=", programGroupCommandLine, spaceSeparated=false) +
    optional("PROGRAM_GROUP_NAME=", programGroupName, spaceSeparated=false) +
    optional("COMMENT=", comment, spaceSeparated=false) +
    conditional(removeDuplicates, "REMOVE_DUPLICATES=TRUE") + 
    conditional(assumeSorted, "ASSUME_SORTED=TRUE") + 
    (if (maxSequencesForDiskReadEndsMap > 0) optional("MAX_SEQUENCES_FOR_DISK_READ_ENDS_MAP=", maxSequencesForDiskReadEndsMap, spaceSeparated=false) else "") +
    (if (maxFileHandlesForReadEndsMap > 0) optional("MAX_FILE_HANDLES_FOR_READ_ENDS_MAP=", maxFileHandlesForReadEndsMap, spaceSeparated=false) else "") +
    (if (sortingCollectionSizeRatio > 0) optional("SORTING_COLLECTION_SIZE_RATIO=", sortingCollectionSizeRatio, spaceSeparated=false) else "") +
    optional("READ_NAME_REGEX=", readNameRegex, spaceSeparated=false) +
    (if (opticalDuplicatePixelDistance > 0) optional("OPTICAL_DUPLICATE_PIXEL_DISTANCE=", opticalDuplicatePixelDistance, spaceSeparated=false) else "")
}
 object MarkDuplicates {
   def apply(root:Configurable, input:List[File], outputDir:String) : MarkDuplicates = {
    val markDuplicates = new MarkDuplicates(root)
    markDuplicates.input = input
    markDuplicates.output = new File(outputDir, input.head.getName.stripSuffix(".bam") + ".dedup.bam")
    markDuplicates.outputMetrics = new File(outputDir, input.head.getName.stripSuffix(".bam") + ".dedup.metrics")
    return markDuplicates
  }
  
  def apply(root:Configurable, input:List[File], output:File) : MarkDuplicates = {
    val markDuplicates = new MarkDuplicates(root)
    markDuplicates.input = input
    markDuplicates.output = output
    markDuplicates.outputMetrics = new File(output.getParent, output.getName.stripSuffix(".bam") + ".metrics")
    return markDuplicates
  }
 }