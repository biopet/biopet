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

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * @deprecated Use picard.util.BedToIntervalList instead
  */
class BedToInterval(val parent: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.BedToInterval

  @Input(doc = "Input Bed file", required = true)
  var input: File = _

  @Input(doc = "Bam File", required = true)
  var bamFile: File = _

  @Output(doc = "Output interval list", required = true)
  var output: File = _

  override def defaultCoreMemory = 1.0

  override def cmdLine =
    super.cmdLine + required("-I", input) + required("-b", bamFile) + required("-o", output)
}

object BedToInterval {
  def apply(root: Configurable, inputBed: File, inputBam: File, output: File): BedToInterval = {
    val bedToInterval = new BedToInterval(root)
    bedToInterval.input = inputBed
    bedToInterval.bamFile = inputBam
    bedToInterval.output = output
    bedToInterval
  }
}
