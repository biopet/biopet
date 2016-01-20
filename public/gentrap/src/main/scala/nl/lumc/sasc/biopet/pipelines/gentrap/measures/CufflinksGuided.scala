package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
class CufflinksGuided(val root: Configurable) extends QScript with Measurement {
  def bamToCountFile(id: String, bamFile: File): (String, File) = ???

  def mergeArgs = MergeArgs(List(1, 7), 10, numHeaderLines = 1, fallback = "0.0")
}
