package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

class SortSam(val root: Configurable) extends Picard {
  javaMainClass = "picard.sam.SortSam"

  @Input(doc = "The input SAM or BAM files to analyze.", required = true)
  var input: File = _

  @Output(doc = "The output file to bam file to", required = true)
  var output: File = _

  @Argument(doc = "Sort order of output file Required. Possible values: {unsorted, queryname, coordinate} ", required = true)
  var sortOrder: String = _

  @Output(doc = "Bam Index", required = true)
  private var outputIndex: File = _

  override def afterGraph {
    super.afterGraph
    if (createIndex) outputIndex = new File(output.getAbsolutePath.stripSuffix(".bam") + ".bai")
  }

  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    required("SORT_ORDER=", sortOrder, spaceSeparated = false)
}

object SortSam {
  def apply(root: Configurable, input: File, output: File, sortOrder: String = null): SortSam = {
    val sortSam = new SortSam(root)
    sortSam.input = input
    sortSam.output = output
    if (sortOrder == null) sortSam.sortOrder = "coordinate"
    else sortSam.sortOrder = sortOrder
    return sortSam
  }
}