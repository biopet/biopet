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
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions.sambamba

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/** Extension for sambemba markdup  */
class SambambaMarkdup(val root: Configurable) extends Sambamba {
  override def defaultThreads = 4

  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "Markdup output bam")
  var output: File = _

  var removeDuplicates: Boolean = config("remove_duplicates", default = false)

  // @doc: compression_level 6 is average, 0 = no compression, 9 = best
  val compressionLevel: Option[Int] = config("compression_level", default = 6)
  val hashTableSize: Option[Int] = config("hash-table-size", default = 262144)
  val overflowListSize: Option[Int] = config("overflow-list-size", default = 200000)
  val ioBufferSize: Option[Int] = config("io-buffer-size", default = 128)

  /** Returns command to execute */
  def cmdLine = required(executable) +
    required("markdup") +
    conditional(removeDuplicates, "--remove-duplicates") +
    optional("-t", nCoresRequest) +
    optional("-l", compressionLevel) +
    optional("--hash-table-size=", hashTableSize, spaceSeparated = false) +
    optional("--overflow-list-size=", overflowListSize, spaceSeparated = false) +
    optional("--io-buffer-size=", ioBufferSize, spaceSeparated = false) +
    required(input) +
    required(output)
}

object SambambaMarkdup {
  def apply(root: Configurable, input: File, output: File): SambambaMarkdup = {
    val markdup = new SambambaMarkdup(root)
    markdup.input = input
    markdup.output = output
    markdup
  }

  def apply(root: Configurable, input: File): SambambaMarkdup = {
    apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".dedup.bam"
}
