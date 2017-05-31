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

class SageCreateTagCounts(val parent: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.SageCreateTagCounts

  @Input(doc = "Raw count file", shortName = "input", required = true)
  var input: File = _

  @Input(doc = "tag library", shortName = "taglib", required = true)
  var tagLib: File = _

  @Output(doc = "Sense count file", shortName = "sense", required = true)
  var countSense: File = _

  @Output(doc = "Sense all count file", shortName = "allsense", required = true)
  var countAllSense: File = _

  @Output(doc = "AntiSense count file", shortName = "antisense", required = true)
  var countAntiSense: File = _

  @Output(doc = "AntiSense all count file", shortName = "allantisense", required = true)
  var countAllAntiSense: File = _

  override def defaultCoreMemory = 3.0

  override def cmdLine =
    super.cmdLine +
      required("-I", input) +
      required("--tagLib", tagLib) +
      optional("--countSense", countSense) +
      optional("--countAllSense", countAllSense) +
      optional("--countAntiSense", countAntiSense) +
      optional("--countAllAntiSense", countAllAntiSense)
}
