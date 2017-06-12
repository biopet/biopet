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
package nl.lumc.sasc.biopet.pipelines.bammetrics.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.extensions.PythonCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

class CoverageStats(val parent: Configurable) extends PythonCommandLineFunction with Summarizable {
  setPythonScript("nl/lumc/sasc/biopet/pipelines/bammetrics/scripts/bedtools_cov_stats.py")

  @Input(doc = "Input file", required = false)
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  @Output(doc = "plot File (png)")
  var plot: File = _

  var title: Option[String] = None
  var subTitle: Option[String] = None

  override def defaultCoreMemory = 9.0

  def cmdLine =
    getPythonCommand +
      (if (inputAsStdin) " - " else required(input)) +
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
  def apply(root: Configurable, outputDir: File, name: String): CoverageStats = {
    val coverageStats = new CoverageStats(root)
    coverageStats.output = new File(outputDir, name + ".stats")
    coverageStats.plot = new File(outputDir, name + ".stats.png")
    coverageStats
  }
}
