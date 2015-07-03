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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class SquishBed(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("bed_squish.py")

  @Input(doc = "Input file")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  def cmdLine = getPythonCommand +
    required(input) +
    required(output)
}

object SquishBed {
  def apply(root: Configurable, input: File, outputDir: File): SquishBed = {
    val squishBed = new SquishBed(root)
    squishBed.input = input
    squishBed.output = new File(outputDir, input.getName.stripSuffix(".bed") + ".squish.bed")
    squishBed
  }
}
