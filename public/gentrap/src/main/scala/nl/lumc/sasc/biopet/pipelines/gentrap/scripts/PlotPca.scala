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
package nl.lumc.sasc.biopet.pipelines.gentrap.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.pipelines.gentrap.extensions.RScriptCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Wrapper for the plot_pca.R script, used internally in Gentrap
 */
class PlotPca(val root: Configurable) extends RScriptCommandLineFunction {

  setRScript("plot_pca.R", "/nl/lumc/sasc/biopet/pipelines/gentrap/scripts/")

  @Input(doc = "Input table", required = true)
  var input: File = null

  @Output(doc = "Output plot", required = false)
  var output: File = null

  var tmmNormalize: Boolean = config("tmm_normalize", default = false)

  def cmdLine = {
    RScriptCommand +
      conditional(tmmNormalize, "-T") +
      required("-I", input) +
      required("-O", output)
  }
}
