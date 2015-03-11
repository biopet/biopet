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
