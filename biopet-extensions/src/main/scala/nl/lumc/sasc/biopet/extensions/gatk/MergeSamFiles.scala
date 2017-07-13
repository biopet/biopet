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
package nl.lumc.sasc.biopet.extensions.gatk

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.extensions.picard
import org.broadinstitute.gatk.queue.function.scattergather.{GatherFunction, ScatterGatherableFunction}

/**
 * Merges BAM files using htsjdk.samtools.MergeSamFiles.
 */
class MergeSamFiles extends picard.MergeSamFiles(null) with GatherFunction {

  override val parent: ScatterGatherableFunction with BiopetCommandLineFunction = originalFunction match {
    case b: BiopetCommandLineFunction => b
    case _                            => null
  }

  this.assumeSorted = true

  override def freezeFieldValues() {
    this.input = this.gatherParts.toList
    this.output = this.originalOutput
    this.sortOrder = "coordinate"
    //Left to its own devices (ie, MergeSamFiles.freezeFieldValues), outputIndex
    //will be in the gather directory.  Ensure that it actually matches this.output

    val originalGATK = originalFunction.asInstanceOf[CommandLineGATK]

    // Whatever the original function can handle, merging *should* do less.
    this.createIndex = !originalGATK.disable_bam_indexing

    super.freezeFieldValues()
  }
}
