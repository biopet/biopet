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
package nl.lumc.sasc.biopet.extensions.freec

import java.io.File

import nl.lumc.sasc.biopet.core.extensions.{ PythonCommandLineFunction, RscriptCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class FreeCCNVPlot(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("freec_CNVPlot.py")

  @Input(doc = "Output file from FreeC. *_CNV", required = true)
  var input: File = null

  @Output(doc = "Destination for the PNG file", required = true)
  var output: File = null

  override def cmdLine = getPythonCommand +
    required("-I", input) +
    required("-O", output)
}
