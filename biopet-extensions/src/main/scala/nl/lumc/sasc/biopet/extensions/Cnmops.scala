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

import nl.lumc.sasc.biopet.core.extensions.RscriptCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline._

/**
 * Wrapper for the Cnmops command line tool.
 * Written based on Cnmops version v2.2.1.
 */
class Cnmops(val root: Configurable) extends RscriptCommandLineFunction {

  override def defaultThreads = 4
  override def defaultCoreMemory: Double = 4.0

  protected var script: File = new File("/nl/lumc/sasc/biopet/extensions/cnmops.R")

  @Input(doc = "Input file BAM", required = true)
  var input: List[File] = List()

  @Argument(doc = "Chromsomosome to query", required = true)
  var chromosome: String = _

  @Argument(doc = "Window length", required = false)
  var windowLength: Int = config("window_length", namespace = "kopisu", default = 1000)

  // output files, computed automatically from output directory
  @Output(doc = "Output CNV file")
  lazy val outputCnv: File = {
    require(!outputDir.isEmpty, "Unexpected error when trying to set cn.MOPS CNV output")
    new File(outputDir, "cnv.txt")
  }

  @Output(doc = "Output CNR file")
  lazy val outputCnr: File = {
    require(!outputDir.isEmpty, "Unexpected error when trying to set cn.MOPS CNR output")
    new File(outputDir, "cnr.txt")
  }

  @Output(doc = "Raw output")
  lazy val rawOutput: File = {
    require(!outputDir.isEmpty, "Unexpected error when trying to set cn.MOPS raw output")
    new File(outputDir, "rawoutput.txt")
  }

  /** write all output files to this directory [./] */
  var outputDir: String = _

  override def beforeGraph = {
    super.beforeGraph
    require(!outputDir.isEmpty, "Outputdir for cn.MOPS should not be empty")
    require(input.length >= 2, "Please supply at least 2 BAM files for cn.MOPS")
  }

  override def cmdLine = super.cmdLine +
    required("--cnr", outputCnr) +
    required("--cnv", outputCnv) +
    required("--chr", chromosome) +
    required("--rawoutput", rawOutput) +
    required("--threads", threads) +
    optional("--wl", windowLength) +
    required(input.map(f => f.getAbsolutePath).mkString(" "))
}
