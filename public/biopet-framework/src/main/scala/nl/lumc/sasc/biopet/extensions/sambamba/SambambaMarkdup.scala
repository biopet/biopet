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
    val markdup = new SambambaMarkdup(root)
    markdup.input = input
    markdup.output = output
    return markdup
  }

  def apply(root: Configurable, input: File): SambambaMarkdup = {
    return apply(root, input, new File(swapExtension(input.getCanonicalPath)))
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".dedup.bam"
}