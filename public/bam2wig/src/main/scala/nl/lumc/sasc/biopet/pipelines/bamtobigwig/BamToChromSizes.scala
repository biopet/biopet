package nl.lumc.sasc.biopet.pipelines.bamtobigwig

import java.io.{ PrintWriter, File }

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }
import scala.collection.JavaConversions._

/**
 * Created by pjvan_thof on 1/29/15.
 */
class BamToChromSizes(val root: Configurable) extends InProcessFunction with Configurable {
  @Input
  var bamFile: File = _

  @Output
  var chromSizesFile: File = _

  def run(): Unit = {
    val bamReader = SamReaderFactory.makeDefault().open(bamFile)
    val writer = new PrintWriter(chromSizesFile)
    for (ref <- bamReader.getFileHeader.getSequenceDictionary.getSequences) {
      writer.println(ref.getSequenceName + "\t" + ref.getSequenceLength)
    }
    bamReader.close()
    writer.close
  }
}
