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

/**
 * Created by waiyileung on 05-10-15.
 */
import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * KrakenReportToJson function class for usage in Biopet pipelines
 *
 * @param root Configuration object for the pipeline
 */
class KrakenReportToJson(val root: Configurable) extends ToolCommandFunction with Summarizable {
  def toolObject = nl.lumc.sasc.biopet.tools.KrakenReportToJson

  @Input(doc = "Input Kraken Full report", shortName = "inputReport", required = true)
  var inputReport: File = _

  @Argument(required = false)
  var skipNames: Boolean = false

  @Output(doc = "Output JSON", shortName = "output", required = true)
  var output: File = _

  override def defaultCoreMemory = 2.0

  override def cmdLine =
    super.cmdLine +
      required("-i", inputReport) +
      required("-o", output) +
      conditional(skipNames, "--skipnames")

  def summaryStats: Map[String, Any] = {
    ConfigUtils.fileToConfigMap(output)
  }

  def summaryFiles: Map[String, File] = Map()

}

object KrakenReportToJson {
  def apply(root: Configurable, input: File, output: File): KrakenReportToJson = {
    val report = new KrakenReportToJson(root)
    report.inputReport = input
    report.output = new File(output, input.getName.substring(0, input.getName.lastIndexOf(".")) + ".kraken.json")
    report
  }
}

