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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.ToolCommandFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Queue extension for the FastqSplitter
 * @param root Parent object
 */
class FastqSplitter(val root: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.FastqSplitter

  @Input(doc = "Input fastq", shortName = "input", required = true)
  var input: File = _

  @Output(doc = "Output fastq files", shortName = "output", required = true)
  var output: List[File] = Nil

  override def defaultCoreMemory = 4.0

  /** * Generate command to execute */
  override def cmdLine = super.cmdLine +
    required("-I", input) +
    repeat("-o", output)
}
