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

class NcbiReportToContigMap(val parent: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.NcbiReportToContigMap

  @Output(doc = "Output fasta file", required = true)
  var contigMap: File = _

  var outputReport: File = _

  @Input(required = true)
  var assemblyReport: File = _

  var nameHeader: String = _

  override def defaultCoreMemory = 4.0

  override def cmdLine =
    super.cmdLine +
      required("-a", assemblyReport) +
      required("--report", outputReport) +
      required("-o", contigMap) +
      required("--nameHeader", nameHeader)
}
