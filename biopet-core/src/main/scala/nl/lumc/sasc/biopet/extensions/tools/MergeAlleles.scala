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

class MergeAlleles(val parent: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.MergeAlleles

  @Input(doc = "Input vcf files", shortName = "input", required = true)
  var input: List[File] = Nil

  @Output(doc = "Output vcf file", shortName = "output", required = true)
  var output: File = _

  @Output(doc = "Output vcf file index", shortName = "output", required = true)
  private var outputIndex: File = _

  var reference: File = config("reference")

  override def defaultCoreMemory = 1.0

  override def beforeGraph() {
    super.beforeGraph()
    if (output.getName.endsWith(".gz")) outputIndex = new File(output.getAbsolutePath + ".tbi")
    if (output.getName.endsWith(".vcf")) outputIndex = new File(output.getAbsolutePath + ".idx")
  }

  override def cmdLine =
    super.cmdLine +
      repeat("-I", input) +
      required("-o", output) +
      required("-R", reference)
}

object MergeAlleles {
  def apply(root: Configurable, input: List[File], output: File): MergeAlleles = {
    val mergeAlleles = new MergeAlleles(root)
    mergeAlleles.input = input
    mergeAlleles.output = output
    mergeAlleles
  }
}
