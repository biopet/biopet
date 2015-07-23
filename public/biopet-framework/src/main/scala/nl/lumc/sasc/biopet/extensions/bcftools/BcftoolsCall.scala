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
package nl.lumc.sasc.biopet.extensions.bcftools

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/** This extension is based on bcftools 1.1-134 */
class BcftoolsCall(val root: Configurable) extends Bcftools {
  @Input(doc = "Input File")
  var input: File = _

  @Output(doc = "output File")
  var output: File = _

  var O: String = null
  var v: Boolean = config("v", default = true)
  var c: Boolean = config("c", default = false)
  var m: Boolean = config("m", default = false)

  override def beforeGraph(): Unit = {
    require(c != m)
  }

  def cmdBase = required(executable) +
    required("call") +
    optional("-O", O) +
    conditional(v, "-v") +
    conditional(c, "-c") +
    conditional(m, "-m")
  def cmdPipeInput = cmdBase + "-"
  def cmdPipe = cmdBase + input
  def cmdLine = cmdPipe + " > " + required(output)
}
