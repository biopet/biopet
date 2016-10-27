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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.Version
import nl.lumc.sasc.biopet.core.extensions.RscriptCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline._

import nl.lumc.sasc.biopet.utils.getSemanticVersion

/**
 * Wrapper for the Cnmops command line tool.
 * Written based on Cnmops version v2.2.1.
 */
class Cnmops(val root: Configurable) extends RscriptCommandLineFunction with Version {

  override def defaultThreads = 4
  override def defaultCoreMemory: Double = 4.0

  protected var script: File = new File("/nl/lumc/sasc/biopet/extensions/cnmops.R")

  def versionCommand = {
    val v = super.cmdLine + "--version"
    v.trim.replace("'", "")
  }
  def versionRegex = "(\\d+\\.\\d+\\.\\d+)".r

  private def stringToInt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case e: Exception => None
    }
  }

  /**
   * Check whether version of cn mops is at least 1.18.0
   *
   * @return
   */
  def versionCheck: Boolean = {
    getVersion.flatMap(getSemanticVersion(_)) match {
      case Some(version) => (version.major == 1 && version.minor >= 18) || version.major >= 2
      case _             => false
    }
  }

  @Input(doc = "Input file BAM", required = true)
  var input: List[File] = List()

  @Argument(doc = "Chromsomosome to query", required = true)
  var chromosome: String = _

  @Argument(doc = "Window length", required = false)
  var windowLength: Int = config("window_length", namespace = "kopisu", default = 1000)

  // output files, computed automatically from output directory
  @Output(doc = "Output CNV file")
  lazy val outputCnv: File = {
    outputDir match {
      case Some(dir) => new File(dir, "cnv.txt")
      case _         => throw new IllegalArgumentException("Unexpected error when trying to set cn.MOPS CNV output")
    }
  }

  @Output(doc = "Output CNR file")
  lazy val outputCnr: File = {
    outputDir match {
      case Some(dir) => new File(dir, "cnr.txt")
      case _         => throw new IllegalArgumentException("Unexpected error when trying to set cn.MOPS CNR output")
    }
  }

  @Output(doc = "Raw output")
  lazy val rawOutput: File = {
    outputDir match {
      case Some(dir) => new File(dir, "rawoutput.txt")
      case _         => throw new IllegalArgumentException("Unexpected error when trying to set cn.MOPS raw output")
    }
  }

  /** write all output files to this directory [./] */
  var outputDir: Option[File] = None

  override def beforeGraph = {
    super.beforeGraph
    require(outputDir.isDefined, "Outputdir for cn.MOPS should not be empty")
    require(input.length >= 2, "Please supply at least 2 BAM files for cn.MOPS")
    if (!versionCheck) {
      logger.warn("cn.mops version is below 1.18.0. Contigs containing little to no reads WILL fail")
    }
  }

  override def cmdLine = super.cmdLine +
    required("--cnr", outputCnr) +
    required("--cnv", outputCnv) +
    required("--chr", chromosome) +
    required("--rawoutput", rawOutput) +
    required("--threads", threads) +
    optional("--wl", windowLength) +
    repeat(input)
}
