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
package nl.lumc.sasc.biopet.pipelines.mapping.scripts

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.extensions.PythonCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Wrapper for the tophat-recondition.py script.
  *
  * NOTE: we are modifying the input and output to be the BAM files directly so the wrapper works nice with Queue.
  */
class TophatRecondition(val parent: Configurable) extends PythonCommandLineFunction {

  setPythonScript("tophat-recondition.py")

  @Input(doc = "Path to input accepted_hits.bam", required = true)
  var inputBam: File = _

  @Output(doc = "Path to output unmapped_fixup.bam", required = false)
  var outputSam: File = _

  private def inputDir: File = inputBam.getAbsoluteFile.getParentFile

  private def outputDir: File = outputSam.getAbsoluteFile.getParentFile

  override def beforeGraph(): Unit = {
    require(inputBam != null, "Input must be defined.")
    require(outputSam != null, "Output must be defined.")
  }

  def cmdLine = getPythonCommand + required(inputDir) + required(outputDir)
}
