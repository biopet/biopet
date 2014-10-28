package nl.lumc.sasc.biopet.extensions.sambamba

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class SambambaMarkdup(val root: Configurable) extends Sambamba {
  override val defaultThreads = 4

  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "Markdup output bam")
  var output: File = _

  var remove_duplicates: Boolean = config("remove_duplicates", default = false)

  // @doc: compression_level 6 is average, 0 = no compression, 9 = best
  val compression_level: Option[Int] = config("compression_level", default = 6)
  val hash_table_size: Option[Int] = config("hash-table-size", default = 262144)
  val overflow_list_size: Option[Int] = config("overflow-list-size", default = 200000)
  val io_buffer_size: Option[Int] = config("io-buffer-size", default = 128)

  def cmdLine = required(executable) +
    required("markdup") +
    conditional(remove_duplicates, "--remove-duplicates") +
    optional("-t", nCoresRequest) +
    optional("-l", compression_level) +
    optional("--hash-table-size=", hash_table_size, spaceSeparated = false) +
    optional("--overflow-list-size=", overflow_list_size, spaceSeparated = false) +
    optional("--io-buffer-size=", io_buffer_size, spaceSeparated = false) +
    required(input) +
    required(output)
}

object SambambaMarkdup {
  def apply(root: Configurable, input: File, output: File): SambambaMarkdup = {
    val flagstat = new SambambaMarkdup(root)
    flagstat.input = input
    flagstat.output = output
    return flagstat
  }

  def apply(root: Configurable, input: File, outputDir: String): SambambaMarkdup = {
    val dir = if (outputDir.endsWith("/")) outputDir else outputDir + "/"
    val outputFile = new File(dir + swapExtension(input.getName))
    return apply(root, input, outputFile)
  }

  def apply(root: Configurable, input: File): SambambaMarkdup = {
    return apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".bam.bai"
}