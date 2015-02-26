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
import nl.lumc.sasc.biopet.core.config.{ ConfigValueIndex, Config, Configurable }
import org.broadinstitute.gatk.utils.commandline.Argument
import org.broadinstitute.gatk.queue.QSettings
import org.broadinstitute.gatk.queue.function.QFunction
import org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction
import org.broadinstitute.gatk.queue.util.{ Logging => GatkLogging }
import scala.collection.mutable.ListBuffer

/**
 * Base for biopet pipeline
 */
trait BiopetQScript extends Configurable with GatkLogging {

  @Argument(doc = "JSON config file(s)", fullName = "config_file", shortName = "config", required = false)
  val configfiles: List[File] = Nil

  var outputDir: File = {
    Config.getValueFromMap(globalConfig.map, ConfigValueIndex(this.configName, configPath, "output_dir")) match {
      case Some(value) => new File(value.asString).getAbsoluteFile
      case _           => new File(".")
    }
  }

  @Argument(doc = "Disable all scatters", shortName = "DSC", required = false)
  var disableScatter: Boolean = false

  var outputFiles: Map[String, File] = Map()

  /** Get implemented from org.broadinstitute.gatk.queue.QScript */
  var qSettings: QSettings

  /** Get implemented from org.broadinstitute.gatk.queue.QScript */
  var functions: Seq[QFunction]

  /** Init for pipeline */
  def init

  /** Pipeline itself */
  def biopetScript

  /**
   * Script from queue itself, final to force some checks for each pipeline and write report
   */
  final def script() {
    outputDir = config("output_dir").asFile.getAbsoluteFile
    init
    biopetScript

    if (disableScatter) for (function <- functions) function match {
      case f: ScatterGatherableFunction => f.scatterCount = 1
      case _                            =>
    }
    for (function <- functions) function match {
      case f: BiopetCommandLineFunctionTrait => {
        f.preProcesExecutable
        f.beforeGraph
        f.commandLine
      }
      case _ =>
    }

    if (outputDir.getParentFile.canWrite || (outputDir.exists && outputDir.canWrite))
      globalConfig.writeReport(qSettings.runName, new File(outputDir, ".log/" + qSettings.runName))
    else BiopetQScript.addError("Parent of output dir: '" + outputDir.getParent + "' is not writeable, outputdir can not be created")

    BiopetQScript.checkErrors
  }

  /** Get implemented from org.broadinstitute.gatk.queue.QScript */
  def add(functions: QFunction*)

  /** Get implemented from org.broadinstitute.gatk.queue.QScript */
  def addAll(functions : scala.Traversable[org.broadinstitute.gatk.queue.function.QFunction])

  /**
   * Function to set isIntermediate and add in 1 line
   * @param function
   * @param isIntermediate
   */
  def add(function: QFunction, isIntermediate: Boolean = false) {
    function.isIntermediate = isIntermediate
    add(function)
  }
}

object BiopetQScript extends Logging {
  private val errors: ListBuffer[Exception] = ListBuffer()

  def addError(error: String, debug: String = null): Unit = {
    val msg = error + (if (debug != null && logger.isDebugEnabled) "; " + debug else "")
    errors.append(new Exception(msg))
  }

  protected def checkErrors: Unit = {
    if (!errors.isEmpty) {
      logger.error("*************************")
      logger.error("Biopet found some errors:")
      if (logger.isDebugEnabled) {
        for (e <- errors) {
          logger.error(e.getMessage)
          logger.debug(e.getStackTrace.mkString("Stack trace:\n", "\n", "\n"))
        }
      } else {
        errors.map(_.getMessage).sorted.distinct.foreach(logger.error(_))
      }
      throw new IllegalStateException("Biopet found errors")
    }
  }
}
