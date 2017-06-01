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
import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.tools.flagstat
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

class BiopetFlagstat(val parent: Configurable) extends ToolCommandFunction with Summarizable {
  def toolObject = flagstat.BiopetFlagstat

  @Input(doc = "Input bam", shortName = "input", required = true)
  var input: File = _

  @Output(doc = "Output flagstat", shortName = "output", required = true)
  var output: File = _

  @Output(doc = "summary output file", shortName = "output", required = false)
  var summaryFile: File = _

  override def defaultCoreMemory = 6.0

  override def cmdLine =
    super.cmdLine + required("-I", input) + required("-s", summaryFile) + " > " + required(output)

  def summaryFiles: Map[String, File] = Map()

  def summaryStats: Map[String, Any] = {
    ConfigUtils.fileToConfigMap(summaryFile)
  }
}

object BiopetFlagstat {
  def apply(root: Configurable, input: File, outputDir: File): BiopetFlagstat = {
    val flagstat = new BiopetFlagstat(root)
    flagstat.input = input
    flagstat.output = new File(outputDir, input.getName.stripSuffix(".bam") + ".biopetflagstat")
    flagstat.summaryFile =
      new File(outputDir, input.getName.stripSuffix(".bam") + ".biopetflagstat.json")
    flagstat
  }
}
