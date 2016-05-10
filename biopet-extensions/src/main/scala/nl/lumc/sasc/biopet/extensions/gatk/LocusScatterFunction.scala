package nl.lumc.sasc.biopet.extensions.gatk

import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.interval.IntervalUtils

import scala.collection.JavaConversions._

/**
 * A scatter function that divides down to the locus level.
 */
class LocusScatterFunction extends GATKScatterFunction with InProcessFunction {
  protected override def maxIntervals = scatterCount

  def run() {
    val gi = GATKScatterFunction.getGATKIntervals(this.originalGATK)
    val splits = IntervalUtils.splitLocusIntervals(gi.locs, this.scatterOutputFiles.size)
    IntervalUtils.scatterFixedIntervals(gi.samFileHeader, splits, this.scatterOutputFiles)
  }
}