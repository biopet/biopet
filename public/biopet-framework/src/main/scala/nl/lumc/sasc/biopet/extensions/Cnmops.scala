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
import nl.lumc.sasc.biopet.core.{BiopetJavaCommandLineFunction, BiopetCommandLineFunction}
import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * Wrapper for the Cnmops command line tool.
 * Written based on Cnmops version v2.2.1.
 */
class Cnmops(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  /** input file */
  @Input(doc = "Input file BAM", required = true)
  var input: List[File] = List()

  /** output files, computed automatically from output directory */

  @Output(doc = "Output CNV file")
  lazy val output_cnv: File = {
    if (output_dir == null)
      throw new RuntimeException("Unexpected error when trying to set Cnmops CNV output")
    new File(output_dir,  "cnv.txt")
  }
  @Output(doc = "Output CNR file")
  lazy val output_cnr: File = {
    if (output_dir == null)
      throw new RuntimeException("Unexpected error when trying to set Cnmops CNR output")
    new File(output_dir,  "cnr.txt")
  }

  /** write all output files to this directory [./] */
  var output_dir: String = _

  override def beforeGraph = {
    super.beforeGraph
    require(!output_dir.isEmpty, "Outputdir for cn.MOPS should not be empty")
    require(input.length > 1, "Please supply at least 2 BAM files for cn.MOPS")
  }

  override val versionRegex = """Cnmops v(.*)""".r
  override def versionCommand = executable

  def cmdLine = {
    required(executable) +
      required("--output-dir", output_dir) +
      required(input)
  }
}
