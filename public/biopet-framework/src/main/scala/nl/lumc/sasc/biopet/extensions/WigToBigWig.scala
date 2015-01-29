package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 1/29/15.
 * Versions from the executable are not reliable, this extension is based on md5 '3d033ff8a1f4ea9cb3ede12939561141' from the executable
 */
class WigToBigWig(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Input wig file")
  var inputWigFile: File = _

  @Input(doc = "Input chrom sizes file")
  var inputChromSizesFile: File = _

  @Output(doc = "Output BigWig file")
  var outputBigWig: File = _

  executable = config("exe", default = "wigToBigWig")

  var blockSize: Option[Int] = config("blockSize")
  var itemsPerSlot: Option[Int] = config("itemsPerSlot")
  var clip: Boolean = config("clip", default = false)
  var unc: Boolean = config("unc", default = false)

  def cmdLine = required(executable) +
    optional("-blockSize=", blockSize, spaceSeparated = false) +
    optional("-itemsPerSlot=", itemsPerSlot, spaceSeparated = false) +
    conditional(clip, "-clip") +
    conditional(unc, "-unc") +
    required(inputWigFile) +
    required(inputChromSizesFile) +
    required(outputBigWig)
}