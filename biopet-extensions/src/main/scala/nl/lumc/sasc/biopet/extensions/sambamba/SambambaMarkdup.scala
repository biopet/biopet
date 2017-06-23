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
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/** Extension for sambemba markdup  */
class SambambaMarkdup(val parent: Configurable) extends Sambamba {

  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "Markdup output bam")
  var output: File = _

  var removeDuplicates: Boolean = config("remove_duplicates", default = false)

  // @doc: compression_level 6 is average, 0 = no compression, 9 = best
  val compressionLevel: Option[Int] = config("compression_level")
  val hashTableSize: Option[Int] = config("hash-table-size")
  val overflowListSize: Option[Int] = config("overflow-list-size")
  val ioBufferSize: Option[Int] = config("io-buffer-size")
  val showProgress: Boolean = config("show-progress", default = true)

  override def defaultThreads = 4
  override def defaultCoreMemory = 4.0

  @Output
  private var indexOutput: File = _

  override def beforeGraph(): Unit = {
    indexOutput = new File(output + ".bai")
  }

  /** Returns command to execute */
  def cmdLine: String =
    required(executable) +
      required("markdup") +
      conditional(removeDuplicates, "--remove-duplicates") +
      optional("-t", nCoresRequest) +
      optional("-l", compressionLevel) +
      conditional(showProgress, "--show-progress") +
      optional("--hash-table-size=", hashTableSize, spaceSeparated = false) +
      optional("--overflow-list-size=", overflowListSize, spaceSeparated = false) +
      optional("--io-buffer-size=", ioBufferSize, spaceSeparated = false) +
      required(input) +
      required(output)
}

object SambambaMarkdup {
  def apply(root: Configurable,
            input: File,
            output: File,
            isIntermediate: Boolean = false): SambambaMarkdup = {
    val markdup = new SambambaMarkdup(root)
    markdup.input = input
    markdup.output = output
    markdup.isIntermediate = isIntermediate
    markdup
  }

  def apply(root: Configurable, input: File): SambambaMarkdup = {
    apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".dedup.bam"
}
