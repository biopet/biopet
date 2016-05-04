package nl.lumc.sasc.biopet.extensions.gatk

import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.interval.IntervalUtils

import scala.collection.JavaConversions._

/**
 * Splits intervals by contig instead of evenly.
 */
class ContigScatterFunction extends GATKScatterFunction with InProcessFunction {

  override def scatterCount = if (intervalFilesExist) super.scatterCount min this.maxIntervals else super.scatterCount

  protected override def maxIntervals = {
    GATKScatterFunction.getGATKIntervals(this.originalGATK).contigs.size
  }

  def run() {
    val gi = GATKScatterFunction.getGATKIntervals(this.originalGATK)
    IntervalUtils.scatterContigIntervals(gi.samFileHeader, gi.locs, this.scatterOutputFiles)
  }
}

