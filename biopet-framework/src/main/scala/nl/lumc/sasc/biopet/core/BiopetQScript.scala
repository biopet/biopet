package nl.lumc.sasc.biopet.core

import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.config.{ Config, Configurable }
import org.broadinstitute.gatk.utils.commandline.Argument
import org.broadinstitute.gatk.queue.QSettings
import org.broadinstitute.gatk.queue.function.QFunction

trait BiopetQScript extends Configurable {

  @Argument(doc = "JSON config file(s)", fullName = "config_file", shortName = "config", required = false)
  val configfiles: List[File] = Nil

  @Argument(doc = "Output directory", fullName = "output_directory", shortName = "outDir", required = true)
  var outputDir: String = _

  @Argument(doc = "Use scatter defaults, env vale BIOPET_CONFIG_SCATTER", shortName = "SC", required = false)
  var useScatterDefault: Boolean = false

  var outputFiles: Map[String, File] = Map()

  var qSettings: QSettings

  def init
  def biopetScript

  var functions: Seq[QFunction]

  final def script() {
    if (useScatterDefault) Config.global.loadDefaultScatterConfig
    if (!outputDir.endsWith("/")) outputDir += "/"
    init
    biopetScript
    for (function <- functions) function match {
      case f: BiopetCommandLineFunctionTrait => f.afterGraph
      case _                                 =>
    }
    val configReport = Config.global.getReport
    val configReportFile = new File(outputDir + qSettings.runName + ".configreport.txt")
    configReportFile.getParentFile.mkdir
    val writer = new PrintWriter(configReportFile)
    writer.write(configReport)
    writer.close()
    for (line <- configReport.split("\n")) logger.debug(line)
  }

  def add(functions: QFunction*) // Gets implemeted at org.broadinstitute.sting.queue.QScript
  def add(function: QFunction, isIntermediate: Boolean = false) {
    function.isIntermediate = isIntermediate
    add(function)
  }

}
