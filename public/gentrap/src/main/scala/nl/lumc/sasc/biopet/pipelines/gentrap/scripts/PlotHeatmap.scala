/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.pipelines.gentrap.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.pipelines.gentrap.extensions.RScriptCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Wrapper for the plot_heatmap.R script, used internally in Gentrap
 */
class PlotHeatmap(val root: Configurable) extends RScriptCommandLineFunction {

  setRScript("plot_heatmap.R", "/nl/lumc/sasc/biopet/pipelines/gentrap/scripts/")

  @Input(doc = "Input table", required = true)
  var input: File = null

  @Output(doc = "Output plot", required = false)
  var output: File = null

  var countType: Option[String] = config("count_type")
  var useLog: Boolean = config("use_log", default = false)
  var tmmNormalize: Boolean = config("tmm_normalize", default = false)

  def cmdLine = {
    RScriptCommand +
      conditional(tmmNormalize, "-T") +
      conditional(useLog, "-L") +
      required("-C", countType) +
      required("-I", input) +
      required("-O", output)
  }
}
