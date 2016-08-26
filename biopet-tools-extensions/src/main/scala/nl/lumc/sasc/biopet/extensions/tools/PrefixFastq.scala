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
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Queue class for PrefixFastq tool
 *
 * Created by pjvan_thof on 1/13/15.
 */
class PrefixFastq(val root: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.PrefixFastq

  override def defaultCoreMemory = 1.0

  @Input(doc = "Input fastq", shortName = "I", required = true)
  var inputFastq: File = _

  @Output(doc = "Output fastq", shortName = "o", required = true)
  var outputFastq: File = _

  @Argument(doc = "Prefix seq", required = true)
  var prefixSeq: String = _

  /**
   * Creates command to execute extension
   * @return
   */
  override def cmdLine = super.cmdLine +
    required("-i", inputFastq) +
    required("-o", outputFastq) +
    optional("-s", prefixSeq)
}

object PrefixFastq {
  /**
   * Create a PrefixFastq class object with a sufix ".prefix.fastq" in the output folder
   *
   * @param root parent object
   * @param input input file
   * @param outputDir outputFolder
   * @return PrefixFastq class object
   */
  def apply(root: Configurable, input: File, outputDir: String): PrefixFastq = {
    val prefixFastq = new PrefixFastq(root)
    prefixFastq.inputFastq = input
    prefixFastq.outputFastq = new File(outputDir, input.getName + ".prefix.fastq")
    prefixFastq
  }
}
