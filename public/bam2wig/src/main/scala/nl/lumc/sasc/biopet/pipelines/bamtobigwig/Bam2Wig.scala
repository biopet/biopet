package nl.lumc.sasc.biopet.pipelines.bamtobigwig

import java.io.File

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

  @Input(doc = "Input bam file", required = true)
  var bamFile: File = null

  def init(): Unit = {
  }

  def biopetScript(): Unit = {
    val bs = new BamToChromSizes(this)
    bs.bamFile = bamFile
    bs.chromSizesFile = bamFile.getAbsoluteFile + ".chrom.sizes"
    bs.isIntermediate = true
    add(bs)

    val igvCount = new IGVToolsCount(this)
    igvCount.input = bamFile
    igvCount.genomeChromSizes = bs.chromSizesFile
    igvCount.wig = Some(swapExt(outputDir, bamFile, ".bam", ".wig"))
    igvCount.tdf = Some(swapExt(outputDir, bamFile, ".bam", ".tdf"))
    add(igvCount)

    val wigToBigWig = new WigToBigWig(this)
    wigToBigWig.inputWigFile = igvCount.wig.get
    wigToBigWig.inputChromSizesFile = bs.chromSizesFile
    wigToBigWig.outputBigWig = swapExt(outputDir, bamFile, ".bam", ".bigwig")
    add(wigToBigWig)
  }
}

object Bam2Wig extends PipelineCommand {
  def apply(root: Configurable, bamFile: File): Bam2Wig = {
    val bamToBigWig = new Bam2Wig(root)
    bamToBigWig.outputDir = bamFile.getParent
    bamToBigWig.bamFile = bamFile
    bamToBigWig.init()
    bamToBigWig.biopetScript()
    bamToBigWig
  }
}