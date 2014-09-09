package nl.lumc.sasc.biopet.extensions.samtools

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class SamtoolsFlagstat(val root: Configurable) extends Samtools {
  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  def cmdLine = required(executable) + required("flagstat") + required(input) + " > " + required(output)
}

object SamtoolsFlagstat {
  def apply(root: Configurable, input: File, output: File): SamtoolsFlagstat = {
    val flagstat = new SamtoolsFlagstat(root)
    flagstat.input = input
    flagstat.output = output
    return flagstat
  }

  def apply(root: Configurable, input: File, outputDir: String): SamtoolsFlagstat = {
    val dir = if (outputDir.endsWith("/")) outputDir else outputDir + "/"
    val outputFile = new File(dir + swapExtension(input.getName))
    return apply(root, input, outputFile)
  }

  def apply(root: Configurable, input: File): SamtoolsFlagstat = {
    return apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".flagstat"
}