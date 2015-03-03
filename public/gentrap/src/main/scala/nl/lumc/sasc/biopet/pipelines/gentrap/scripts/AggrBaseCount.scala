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

  setRScript("aggr_base_count.R")

  @Input(doc = "Raw base count files", required = true)
  var inputRawCounts: List[File] = List.empty[File]

  var inputLabels: List[String] = config("input_labels")

  @Output(doc = "Gene level count file", required = false)
  var outputGeneLevelCount: File = null

  @Output(doc = "Exon level count file", required = false)
  var outputExonLevelCount: File = null

  override def beforeGraph: Unit = {
    require(outputExonLevelCount != null || outputGeneLevelCount != null,
      "Either output exon and/or output gene must be set")
  }

  def cmdLine = {
    RScriptCommand +
      required("-I", inputRawCounts.mkString(":")) +
      required("-N", inputLabels.mkString(":")) +
      optional("-G", outputGeneLevelCount) +
      optional("-E", outputExonLevelCount)
  }
}
