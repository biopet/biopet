package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.{ BiopetQScript, SampleLibraryTag }
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.tools.SageCountFastq
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 12/29/15.
 */
class GearsSeqCount(val root: Configurable) extends QScript with BiopetQScript with SampleLibraryTag {

  var fastqInput: File = _

  def countFile = swapExt(outputDir, fastqInput, ".fastq.gz", ".counts.txt")

  /** Init for pipeline */
  def init(): Unit = {
  }

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val seqCount = new SageCountFastq(this)
    seqCount.input = fastqInput
    seqCount.output = countFile
    add(seqCount)
  }
}
