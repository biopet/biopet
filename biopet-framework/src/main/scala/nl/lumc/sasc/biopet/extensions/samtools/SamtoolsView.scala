package nl.lumc.sasc.biopet.extensions.samtools

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class SamtoolsView(val root: Configurable) extends Samtools {
  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  var quality: Option[Int] = config("quality")
  var b: Boolean = config("b")
  var h: Boolean = config("h")

  def cmdBase = required(executable) +
    required("view") +
    optional("-q", quality) +
    conditional(b, "-b") +
    conditional(h, "-h")
  def cmdPipeInput = cmdBase + "-"
  def cmdPipe = cmdBase + required(input)
  def cmdLine = cmdPipe + " > " + required(output)
}

object SamtoolsView {
  def apply(root: Configurable, input: File, output: File): SamtoolsView = {
    val view = new SamtoolsView(root)
    view.input = input
    view.output = output
    return view
  }

  def apply(root: Configurable, input: File, outputDir: String): SamtoolsView = {
    val dir = if (outputDir.endsWith("/")) outputDir else outputDir + "/"
    val outputFile = new File(dir + swapExtension(input.getName))
    return apply(root, input, outputFile)
  }

  def apply(root: Configurable, input: File): SamtoolsView = {
    return apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".mpileup"
}