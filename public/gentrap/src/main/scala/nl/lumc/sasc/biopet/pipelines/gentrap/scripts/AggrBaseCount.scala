/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.pipelines.gentrap.scripts

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.RScriptCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Wrapper for the aggr_base_count.R script, used internally in Gentrap
 */
class AggrBaseCount(val root: Configurable) extends RScriptCommandLineFunction {

  setRScript("aggr_base_count.R", "/nl/lumc/sasc/biopet/pipelines/gentrap/scripts/")

  @Input(doc = "Raw base count files", required = true)
  var input: File = null

  @Output(doc = "Output count file", required = false)
  var output: File = null

  var inputLabel: String = null
  var mode: String = null

  override def beforeGraph: Unit = {
    require(mode == "exon" || mode == "gene", "Mode must be either exon or gene")
    require(input != null, "Input raw base count table must be defined")
  }

  def cmdLine = {
    RScriptCommand +
      required("-I", input) +
      required("-N", inputLabel) +
      optional(if (mode == "gene") "-G" else "-E", output)
  }
}
