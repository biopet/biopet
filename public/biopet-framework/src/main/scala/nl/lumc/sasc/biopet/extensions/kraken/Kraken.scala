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

package nl.lumc.sasc.biopet.extensions.kraken

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/** Extension for Kraken */
class Kraken(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Input: FastQ or FastA")
  var input: List[File] = _

  @Output(doc = "Unidentified reads", required = false)
  var unclassified_out: Option[File] = None

  @Output(doc = "Identified reads", required = false)
  var classified_out: Option[File] = None

  @Output(doc = "Output with hits per sequence")
  var output: File = _

  var db: File = config("db")

  var inputFastQ: Boolean = true
  var compression: Boolean = false
  var compressionGzip: Boolean = false
  var compressionBzip: Boolean = false

  var quick: Boolean = false
  var minHits: Option[Int] = config("min_hits")

  var preLoad: Boolean = config("preload", default = true)
  var paired: Boolean = config("paired", default = false)

  executable = config("exe", default = "kraken")
  override def versionRegex = """Kraken version ([\d\w\-\.]+)\n.*""".r
  override def versionExitcode = List(0, 1)
  override def versionCommand = executable + " --version"

  override def defaultCoreMemory = 8.0
  override def defaultThreads = 4

  /** Sets readgroup when not set yet */
  override def beforeGraph(): Unit = {
    super.beforeGraph()
    //FIXME: This does not do anything
  }

  /** Returns command to execute */
  def cmdLine = required(executable) +
    "--db" + required(db) +
    optional("--threads", nCoresRequest) +
    conditional(inputFastQ, "--fastq-input") +
    conditional(!inputFastQ, "--fasta-input") +
    conditional(quick, "--quick") +
    optional("--min_hits", minHits) +
    optional("--unclassified-out ", unclassified_out.get) +
    optional("--classified-out ", classified_out.get) +
    "--output" + required(output) +
    conditional(preLoad, "--preload") +
    conditional(paired, "--paired") +
    repeat(input)
}
