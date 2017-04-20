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

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

class VcfStatsForSv(val parent: Configurable) extends ToolCommandFunction with Summarizable {
  def toolObject = nl.lumc.sasc.biopet.tools.vcfstats.VcfStatsForSv

  mainFunction = false

  @Input(required = true)
  var inputFile: File = _

  @Argument(required = true)
  var histogramBinBoundaries: Array[Int] = _

  @Output(required = true)
  var outputFile: File = _

  override def defaultCoreMemory = 1.0

  override def cmdLine = super.cmdLine +
    required("-i", inputFile) +
    required("-o", outputFile) +
    repeat("--histBinBoundaries", histogramBinBoundaries)

  def summaryStats: Map[String, Any] = ConfigUtils.yamlToMap(outputFile)

  def summaryFiles: Map[String, File] = Map.empty

}
