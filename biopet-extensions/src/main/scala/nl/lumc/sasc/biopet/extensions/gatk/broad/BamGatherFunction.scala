package nl.lumc.sasc.biopet.extensions.gatk.broad

import org.broadinstitute.gatk.queue.function.scattergather.GatherFunction

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles

/**
 * Merges BAM files using htsjdk.samtools.MergeSamFiles.
 */
class BamGatherFunction extends MergeSamFiles(null) with GatherFunction {

  override val root = originalFunction match {
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
