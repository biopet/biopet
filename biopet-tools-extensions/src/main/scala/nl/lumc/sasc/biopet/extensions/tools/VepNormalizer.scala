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
 * This tool parses a VEP annotated VCF into a standard VCF file.
 * The VEP puts all its annotations for each variant in an CSQ string, where annotations per transcript are comma-separated
 * Annotations are then furthermore pipe-separated.
 * This tool has two modes:
 * 1) explode - explodes all transcripts such that each is on a unique line
 * 2) standard - parse as a standard VCF, where multiple transcripts occur in the same line
 * Created by ahbbollen on 10/27/14.
 */

class VepNormalizer(val parent: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.VepNormalizer

  @Input(doc = "Input VCF, may be indexed", shortName = "InputFile", required = true)
  var inputVCF: File = null

  @Output(doc = "Output VCF", shortName = "OutputFile", required = true)
  var outputVcf: File = null

  var mode: String = config("mode", default = "standard")
  var doNotRemove: Boolean = config("do_not_remove", default = false)

  override def defaultCoreMemory = 4.0

  override def cmdLine = super.cmdLine +
    required("-I", inputVCF) +
    required("-O", outputVcf) +
    required("-m", mode) +
    conditional(doNotRemove, "--do-not-remove")
}
