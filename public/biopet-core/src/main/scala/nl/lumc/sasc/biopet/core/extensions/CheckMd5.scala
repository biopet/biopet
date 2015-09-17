package nl.lumc.sasc.biopet.core.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.summary.WriteSummary
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{Argument, Input}

/**
 * This class checks md5sums and give an exit code 1 when md5sum is not the same
 *
 * Created by pjvanthof on 16/08/15.
 */
class CheckMd5 extends InProcessFunction {
  @Input(required = true)
  var inputFile: File = _

  @Input(required = true)
  var md5file: File = _

  @Argument(required = true)
  var md5sum: String = _

  /** Exits whenever the input md5sum is not the same as the output md5sum */
  def run: Unit = {
    val outputMd5sum = WriteSummary.parseChecksum(md5file).toLowerCase

    if (outputMd5sum != md5sum.toLowerCase) {
      logger.error(s"Input file: '$inputFile' md5sum is not as expected, aborting pipeline")
      System.exit(1)
    }
  }
}