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

/** Extension for sambamba flagstat  */
class SambambaView(val root: Configurable) extends Sambamba {
  override val defaultThreads = 2

  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  var filter: Option[String] = _
  val format: Option[String] = config("format", default = "bam")
  val regions: Option[File] = config("regions")
  val compression_level: Option[Int] = config("compression_level", default = 6)

  /** Returns command to execute */
  def cmdLine = required(executable) +
    required("view") +
    optional("--filter", filter) +
    optional("--nthreads", nCoresRequest) +
    optional("--format", format.get) +
    optional("--regions", regions) +
    optional("--compression-level", compression_level) +
    required("--output" + output) +
    required(input)
}
