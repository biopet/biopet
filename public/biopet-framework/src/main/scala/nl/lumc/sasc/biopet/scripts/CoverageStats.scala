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
import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class CoverageStats(val root: Configurable) extends PythonCommandLineFunction with Summarizable {
  setPythonScript("bedtools_cov_stats.py")

  @Input(doc = "Input file")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  @Output(doc = "plot File (png)")
  var plot: File = _

  var title: Option[String] = None
  var subTitle: Option[String] = None

  override val defaultCoreMemory = 9.0

  def cmdLine = getPythonCommand +
    required(input) +
    required("--plot", plot) +
    optional("--title", title) +
    optional("--subtitle", subTitle) +
    " > " + required(output)

  def summaryFiles: Map[String, File] = Map("plot" -> plot)

  def summaryStats: Map[String, Any] = {
    ConfigUtils.fileToConfigMap(output)
  }
}

object CoverageStats {
  def apply(root: Configurable, input: File, outputDir: File): CoverageStats = {
    val coverageStats = new CoverageStats(root)
    coverageStats.input = input
    coverageStats.output = new File(outputDir, input.getName + ".stats")
    coverageStats.plot = new File(outputDir, input.getName + ".stats.png")
    coverageStats
  }
}
