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
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Created by Sander Bollen on 24-11-16.
 */
class XcnvToBed(val root: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.XcnvToBed

  @Input
  var inputXcnv: File = _

  @Output
  var outpuBed: File = _

  @Argument
  var sample: String = _

  override def defaultCoreMemory = 4

  override def cmdLine = {
    super.cmdLine + required("-I", inputXcnv) + required("-O", outpuBed) + required("-S", sample)
  }

}
