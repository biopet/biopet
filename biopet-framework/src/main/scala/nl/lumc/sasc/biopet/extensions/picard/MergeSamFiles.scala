package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

class MergeSamFiles(val root: Configurable) extends Picard {
  javaMainClass = "picard.sam.MergeSamFiles"

  @Input(doc = "The input SAM or BAM files to analyze.", required = true)
  var input: List[File] = Nil

  @Output(doc = "The output file to bam file to", required = true)
  var output: File = _

  @Argument(doc = "Sort order of output file Required. Possible values: {unsorted, queryname, coordinate} ", required = true)
  var sortOrder: String = _

  @Argument(doc = "ASSUME_SORTED", required = false)
  var assumeSorted: Boolean = config("assumesorted", default = false)
  
  @Argument(doc = "MERGE_SEQUENCE_DICTIONARIES", required = false)
  var mergeSequenceDictionaries: Boolean = config("merge_sequence_dictionaries", default = false)
  
  @Argument(doc = "USE_THREADING", required = false)
  var useThreading: Boolean = config("use_threading", default = false)
  
  @Argument(doc = "COMMENT", required = false)
  var comment: String = config("comment")
  
  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    required("SORT_ORDER=", sortOrder, spaceSeparated = false) +
    conditional(assumeSorted, "ASSUME_SORTED=TRUE") +
    conditional(mergeSequenceDictionaries, "MERGE_SEQUENCE_DICTIONARIES=TRUE") +
    optional("COMMENT=", comment, spaceSeparated = false)
}

object MergeSamFiles {
  def apply(root: Configurable, input: List[File], outputDir: String, sortOrder: String = null): MergeSamFiles = {
    val mergeSamFiles = new MergeSamFiles(root)
    mergeSamFiles.input = input
    mergeSamFiles.output = new File(outputDir, input.head.getName.stripSuffix(".bam").stripSuffix(".sam") + ".merge.bam")
    if (sortOrder == null) mergeSamFiles.sortOrder = "coordinate"
    else mergeSamFiles.sortOrder = sortOrder
    return mergeSamFiles
  }
}