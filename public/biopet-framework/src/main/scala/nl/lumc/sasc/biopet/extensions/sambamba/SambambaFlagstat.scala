package nl.lumc.sasc.biopet.extensions.sambamba

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class SambambaFlagstat(val root: Configurable) extends Sambamba {
  override val defaultThreads = 2

  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  def cmdLine = required(executable) +
    required("flagstat") +
    optional("-t", nCoresRequest) +
    required(input) +
    " > " +
    required(output)
}

object SambambaFlagstat {
  def apply(root: Configurable, input: File, output: File): SambambaFlagstat = {
    val flagstat = new SambambaFlagstat(root)
    flagstat.input = input
    flagstat.output = output
    return flagstat
  }

  def apply(root: Configurable, input: File, outputDir: String): SambambaFlagstat = {
    val dir = if (outputDir.endsWith("/")) outputDir else outputDir + "/"
    val outputFile = new File(dir + swapExtension(input.getName))
    return apply(root, input, outputFile)
  }

  def apply(root: Configurable, input: File): SambambaFlagstat = {
    return apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".flagstat"
}