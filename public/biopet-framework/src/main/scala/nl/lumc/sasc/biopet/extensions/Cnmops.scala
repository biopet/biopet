/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2015 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions

import java.io.File
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import nl.lumc.sasc.biopet.core.{ BiopetJavaCommandLineFunction, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * Wrapper for the Cnmops command line tool.
 * Written based on Cnmops version v2.2.1.
 */
class Cnmops(val root: Configurable) extends RscriptCommandLineFunction {

  protected var script: File = new File("/nl/lumc/sasc/biopet/extensions/cnmops.R")

  @Input(doc = "Input file BAM", required = true)
  var input: List[File] = List()

  // output files, computed automatically from output directory
  @Output(doc = "Output CNV file")
  private lazy val outputCnv: File = {
    if (outputDir == null)
      throw new RuntimeException("Unexpected error when trying to set cn.MOPS CNV output")
    new File(outputDir, "cnv.txt")
  }
  @Output(doc = "Output CNR file")
  private lazy val outputCnr: File = {
    if (outputDir == null)
      throw new RuntimeException("Unexpected error when trying to set cn.MOPS CNR output")
    new File(outputDir, "cnr.txt")
  }

  /** write all output files to this directory [./] */
  var outputDir: String = _

  override def beforeGraph = {
    super.beforeGraph
    require(!outputDir.isEmpty, "Outputdir for cn.MOPS should not be empty")
    require(input.length >= 2, "Please supply at least 2 BAM files for cn.MOPS")
  }

  override def cmdLine = super.cmdLine +
    required(input.foreach(f => f.getAbsolutePath).toString.mkString(" "))
}
