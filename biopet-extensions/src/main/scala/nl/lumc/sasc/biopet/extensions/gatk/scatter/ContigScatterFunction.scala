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
package nl.lumc.sasc.biopet.extensions.gatk.scatter

import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.interval.IntervalUtils

import scala.collection.JavaConversions._

/**
 * Splits intervals by contig instead of evenly.
 */
class ContigScatterFunction extends GATKScatterFunction with InProcessFunction {

  override def scatterCount: Int = if (intervalFilesExist) super.scatterCount min this.maxIntervals else super.scatterCount

  protected override def maxIntervals: Int = {
    GATKScatterFunction.getGATKIntervals(this.originalGATK).contigs.size
  }

  def run() {
    val gi = GATKScatterFunction.getGATKIntervals(this.originalGATK)
    IntervalUtils.scatterContigIntervals(gi.samFileHeader, gi.locs, this.scatterOutputFiles)
  }
}

