package nl.lumc.sasc.biopet.pipelines.bamtobigwig

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions.WigToBigWig
import nl.lumc.sasc.biopet.extensions.igvtools.IGVToolsCount
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 1/29/15.
 */
class Bam2Wig(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input
  var bamFile: File = _

  @Output
  var bigWigFile: File = _

  def init(): Unit = {
  }

  def biopetScript(): Unit = {
    val bs = new BamToChromSizes(this)
    bs.bamFile = bamFile
    bs.chromSizesFile = bamFile.getAbsoluteFile + ".chromsizes"
    bs.isIntermediate = true
    add(bs)

    val igvCount = new IGVToolsCount(this)
    igvCount.input = bamFile
    igvCount.genomeChromSizes = bs.chromSizesFile
    igvCount.wig = Some(swapExt(outputDir, bamFile, ".bam", ".wig"))

    val wigToBigWig = new WigToBigWig(this)
    wigToBigWig.inputWigFile = igvCount.wig.get
    wigToBigWig.inputChromSizesFile = bs.chromSizesFile
    wigToBigWig.outputBigWig = bigWigFile
    add(wigToBigWig)
  }
}

object Bam2Wig extends PipelineCommand