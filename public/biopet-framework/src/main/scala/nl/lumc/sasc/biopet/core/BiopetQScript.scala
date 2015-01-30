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
package nl.lumc.sasc.biopet.core

import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.config.{ Config, Configurable }
import org.broadinstitute.gatk.utils.commandline.Argument
import org.broadinstitute.gatk.queue.QSettings
import org.broadinstitute.gatk.queue.function.QFunction
import org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction
import org.broadinstitute.gatk.queue.util.{ Logging => GatkLogging }

trait BiopetQScript extends Configurable with GatkLogging {

  @Argument(doc = "JSON config file(s)", fullName = "config_file", shortName = "config", required = false)
  val configfiles: List[File] = Nil

  var outputDir: String = _

  @Argument(doc = "Disable all scatters", shortName = "DSC", required = false)
  var disableScatter: Boolean = false

  var outputFiles: Map[String, File] = Map()

  var qSettings: QSettings

  def init
  def biopetScript

  var functions: Seq[QFunction]

  final def script() {
    outputDir = config("output_dir", required = true)
    if (!outputDir.endsWith("/")) outputDir += "/"
    init
    biopetScript

    if (disableScatter) for (function <- functions) function match {
      case f: ScatterGatherableFunction => f.scatterCount = 1
      case _                            =>
    }
    for (function <- functions) function match {
      case f: BiopetCommandLineFunctionTrait => f.afterGraph
      case _                                 =>
    }

    Config.global.writeReport(qSettings.runName, outputDir + ".log/" + qSettings.runName)
  }

  def add(functions: QFunction*) // Gets implemeted at org.broadinstitute.sting.queue.QScript
  def add(function: QFunction, isIntermediate: Boolean = false) {
    function.isIntermediate = isIntermediate
    add(function)
  }

}
