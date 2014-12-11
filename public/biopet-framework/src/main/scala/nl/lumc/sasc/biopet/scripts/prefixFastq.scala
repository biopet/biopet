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

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File

class PrefixFastq(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("prefixFastq.py")

  @Input(doc = "Input file")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  @Argument(doc = "Prefix sequence")
  var prefix: String = "CATG"

  @Argument(doc = "Input file is gziped", required = false)
  var gzip: Boolean = _

  override def beforeCmd {
    if (input.getName.endsWith(".gzip") || input.getName.endsWith("gz")) gzip = true
  }

  def cmdLine = getPythonCommand +
    required("-o", output) +
    required("--prefix", prefix) +
    required(input)
}

object PrefixFastq {
  def apply(root: Configurable, input: File, outputDir: String): PrefixFastq = {
    val prefixFastq = new PrefixFastq(root)
    prefixFastq.input = input
    prefixFastq.output = new File(outputDir, input.getName + ".prefix.fastq")
    return prefixFastq
  }
}
