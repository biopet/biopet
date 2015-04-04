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
package nl.lumc.sasc.biopet.extensions

import java.io.{ FileOutputStream, File }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.Input

import scala.collection.mutable.ListBuffer

/**
 * Created by wyleung on 17-2-15.
 */
trait RscriptCommandLineFunction extends BiopetCommandLineFunction {
  /**
   * Will initially handle Cluster supported commands
   *
   */
  // TODO: write support for running with InProcesFunction

  @Input(doc = "R script", required = false)
  protected var script: File = _
  protected var scriptName: String = _
  protected var arguments: ListBuffer[String] = ListBuffer()
  override val defaultCoreMemory = 2.0

  executable = config("exe", default = "Rscript", submodule = "R")

  /**
   * Adding arguments in order
   *
   * @param argName
   * @param argValue
   * @param dash , is the dashsign a - or -- ?
   * @param sep using a space or "=" to specify the "connector" between argName and argValue
   */
  def addArgument(argName: String, argValue: String, dash: String = "-", sep: String = " ") = {
    arguments += "%s%s%s%s".format(dash, argName, sep, argValue)
  }
  def addPositionalArgument(argValue: String, dash: String = "-", sep: String = " ") = {
    arguments += "%s".format(argValue)
  }

  /**
   * Set the Rscript to run
   *
   * @param filename RScript file location
   */
  def setScript(filename: String): Unit = {
    val f: File = new File(filename)
    f.getAbsoluteFile.exists() match {
      case true => {
        script = f
        scriptName = f.getName
      }
      case false => setScript(f, "")
    }

  }

  /**
   * Gets the R-script from within Biopet
   *
   * throws ResourceNotFound if script doesn't exist
   */
  def setScript(filename: File, subpackage: String): Unit = {
    val RScript: File = new File(".queue/tmp/" + subpackage + filename)
    if (!RScript.getParentFile.exists) RScript.getParentFile.mkdirs

    val is = getClass.getResourceAsStream(subpackage + RScript.getName)
    val os = new FileOutputStream(RScript)

    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()

    script = RScript
    scriptName = RScript.getName
  }

  override def cmdLine: String = {
    required(executable) +
      required(script) +
      arguments.mkString(" ")
  }
}
