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

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction

class FastqcToContams(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("__init__.py", "pyfastqc/")
  setPythonScript("fastqc_contam.py")

  @Input(doc = "Fastqc output", shortName = "fastqc", required = true)
  var fastqc_output: File = _

  @Input(doc = "Contams input", shortName = "fastqc", required = false)
  var contams_file: File = _

  @Output(doc = "Output file", shortName = "out", required = true)
  var out: File = _

  def cmdLine = {
    getPythonCommand +
      required(fastqc_output.getParent()) +
      required("-c", contams_file) +
      " > " +
      required(out)
  }
}
