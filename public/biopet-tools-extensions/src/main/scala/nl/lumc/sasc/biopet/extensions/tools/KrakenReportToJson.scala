package nl.lumc.sasc.biopet.extensions.tools

/**
 * Created by waiyileung on 05-10-15.
 */

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFuntion
import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output, Input }

/**
 * KrakenReportToJson function class for usage in Biopet pipelines
 *
 * @param root Configuration object for the pipeline
 */
class KrakenReportToJson(val root: Configurable) extends ToolCommandFuntion with Summarizable {
  def toolObject = nl.lumc.sasc.biopet.tools.KrakenReportToJson

  @Input(doc = "Input Kraken Full report", shortName = "inputReport", required = true)
  var inputReport: File = null

  @Argument(required = false)
  var skipNames: Boolean = false

  @Output(doc = "Output JSON", shortName = "output", required = true)
  var output: File = null

  override def defaultCoreMemory = 1.0

  override def cmdLine = super.cmdLine +
                      required("-i", inputReport) +
                      required("-o", output) +
                      conditional(skipNames, "--skipnames")

  def summaryStats: Map[String, Any] = {
    val map = ConfigUtils.fileToConfigMap(output)

    ConfigUtils.any2map(map.getOrElse("stats", Map()))
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

  def apply(root: Configurable, input: File, outDir: String): KrakenReportToJson = {
    val report = new KrakenReportToJson(root)
    report.inputReport = input
    report.output = new File(outDir, input.getName.substring(0, input.getName.lastIndexOf(".")) + ".kraken.json")
    report
  }
}

