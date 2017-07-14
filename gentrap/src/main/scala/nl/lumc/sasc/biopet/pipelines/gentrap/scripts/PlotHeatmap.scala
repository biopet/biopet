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
package nl.lumc.sasc.biopet.pipelines.gentrap.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.extensions.RscriptCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Wrapper for the plot_heatmap.R script, used internally in Gentrap
  */
class PlotHeatmap(val parent: Configurable) extends RscriptCommandLineFunction {

  protected var script: File = config("script", default = "plot_heatmap.R")

  @Input(doc = "Input table", required = true)
  var input: File = _

  @Output(doc = "Output plot", required = false)
  var output: File = _

  var countType: Option[String] = config("count_type")
  var useLog: Boolean = config("use_log", default = false)
  var tmmNormalize: Boolean = config("tmm_normalize", default = false)

  override def cmd =
    super.cmd ++
      (if (tmmNormalize) Seq("-T") else Seq()) ++
      (if (useLog) Seq("-L") else Seq()) ++
      (countType match {
        case Some(t) => Seq("-C", t)
        case _ => Seq()
      }) ++
      Seq("-I", input.getAbsolutePath, "-O", output.getAbsolutePath)
}
