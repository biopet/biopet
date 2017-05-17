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

class SageCreateLibrary(val parent: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.SageCreateLibrary

  @Input(doc = "Input fasta", shortName = "input", required = true)
  var input: File = _

  @Output(doc = "Output tag library", shortName = "output", required = true)
  var output: File = _

  @Output(doc = "Output no tags", shortName = "noTagsOutput", required = false)
  var noTagsOutput: File = _

  @Output(doc = "Output no anti tags library", shortName = "noAntiTagsOutput", required = false)
  var noAntiTagsOutput: File = _

  @Output(doc = "Output file all genes", shortName = "allGenes", required = false)
  var allGenesOutput: File = _

  var tag: String = config("tag", default = "CATG")
  var length: Option[Int] = config("length", default = 17)

  override def defaultCoreMemory = 3.0

  override def cmdLine =
    super.cmdLine +
      required("-I", input) +
      optional("--tag", tag) +
      optional("--length", length) +
      optional("--noTagsOutput", noTagsOutput) +
      optional("--noAntiTagsOutput", noAntiTagsOutput) +
      required("-o", output)
}
