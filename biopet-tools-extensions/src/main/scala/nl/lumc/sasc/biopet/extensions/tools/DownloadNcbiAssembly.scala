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
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * @deprecated Use picard.util.BedToIntervalList instead
 */
class DownloadNcbiAssembly(val root: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.DownloadNcbiAssembly

  @Output(doc = "Output fasta file", required = true)
  var output: File = _

  @Output(doc = "Output NCBI report", required = true)
  var outputReport: File = _

  var assemblyId: String = null

  var nameHeader: Option[String] = None

  var mustHaveOne: List[String] = Nil
  var mustNotHave: List[String] = Nil

  override def defaultCoreMemory = 4.0

  override def cmdLine = super.cmdLine +
    required("-a", assemblyId) +
    required("--report", outputReport) +
    required("-o", output) +
    optional("--nameHeader", nameHeader) +
    repeat("--mustHaveOne", mustHaveOne) +
    repeat("--mustNotHave", mustNotHave)
}

