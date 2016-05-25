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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

// TODO: finish implementation for usage in pipelines
/**
 * WipeReads function class for usage in Biopet pipelines
 *
 * @param root Configuration object for the pipeline
 */
class WipeReads(val root: Configurable) extends ToolCommandFunction {

  def toolObject = nl.lumc.sasc.biopet.tools.WipeReads

  @Input(doc = "Input BAM file (must be indexed)", shortName = "I", required = true)
  var inputBam: File = null

  @Input(doc = "Interval file", shortName = "r", required = true)
  var intervalFile: File = null

  @Output(doc = "Output BAM", shortName = "o", required = true)
  var outputBam: File = null

  @Output(doc = "BAM containing discarded reads", shortName = "f", required = false)
  var discardedBam: File = null
}
