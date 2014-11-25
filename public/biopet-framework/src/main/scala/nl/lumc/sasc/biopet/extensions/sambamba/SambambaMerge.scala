package nl.lumc.sasc.biopet.extensions.sambamba

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class SambambaMerge(val root: Configurable) extends Sambamba {
  override val defaultThreads = 4

  @Input(doc = "Bam File[s]")
  var input: List[File] = Nil

  @Output(doc = "Output merged bam PATH")
  var output: File = _

  // @doc: compression_level 6 is average, 0 = no compression, 9 = best
  val compression_level: Option[Int] = config("compression_level", default = 6)

  def cmdLine = required(executable) +
    required("merge") +
    optional("-t", nCoresRequest) +
    optional("-l", compression_level) +
    required(output) +
    repeat("", input)
}

object SambambaMerge {
  def apply(root: Configurable, input: List[File], output: File): SambambaMerge = {
    val flagstat = new SambambaMerge(root)
    flagstat.input = input
    flagstat.output = output
    return flagstat
  }

  def apply(root: Configurable, input: List[File], outputDir: String): SambambaMerge = {
    val dir = if (outputDir.endsWith("/")) outputDir else outputDir + "/"
    val outputFile = new File(dir + swapExtension(input.head.getName))
    return apply(root, input, outputFile)
  }

  def apply(root: Configurable, input: List[File]): SambambaMerge = {
    return apply(root, input, new File(swapExtension(input.head.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".merge.bam"
}

//
//object MergeSamFiles {
//  def apply(root: Configurable, input: List[File], outputDir: String, sortOrder: String = null): MergeSamFiles = {
//    val mergeSamFiles = new MergeSamFiles(root)
//    mergeSamFiles.input = input
//    mergeSamFiles.output = new File(outputDir, input.head.getName.stripSuffix(".bam").stripSuffix(".sam") + ".merge.bam")
//    if (sortOrder == null) mergeSamFiles.sortOrder = "coordinate"
//    else mergeSamFiles.sortOrder = sortOrder
//    return mergeSamFiles
//  }
//}