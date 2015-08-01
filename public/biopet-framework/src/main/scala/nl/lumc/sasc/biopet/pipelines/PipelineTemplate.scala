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
package nl.lumc.sasc.biopet.pipelines

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import org.broadinstitute.gatk.queue.QScript

/** Template for a pipeline */
class PipelineTemplate(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  /** This is executed before the script starts */
  def init() {
  }

  /** Method where jobs must be added */
  def biopetScript() {
  }
}

/** Object to let to generate a main method */
object PipelineTemplate extends PipelineCommand
