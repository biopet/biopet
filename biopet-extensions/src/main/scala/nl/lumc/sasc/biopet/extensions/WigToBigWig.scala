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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by pjvan_thof on 1/29/15.
 * Versions from the executable are not reliable, this extension is based on md5 '3d033ff8a1f4ea9cb3ede12939561141' from the executable
 */
class WigToBigWig(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Input wig file")
  var inputWigFile: File = _

  override def defaultCoreMemory = 3.0

  @Input(doc = "Input chrom sizes file", required = true)
  var inputChromSizesFile: File = _

  @Output(doc = "Output BigWig file", required = true)
  var outputBigWig: File = _

  executable = config("exe", default = "wigToBigWig")

  var blockSize: Option[Int] = config("blockSize")
  var itemsPerSlot: Option[Int] = config("itemsPerSlot")
  var clip: Boolean = config("clip", default = false)
  var unc: Boolean = config("unc", default = false)

  /** Returns command to execute */
  def cmdLine = required(executable) +
    optional("-blockSize=", blockSize, spaceSeparated = false) +
    optional("-itemsPerSlot=", itemsPerSlot, spaceSeparated = false) +
    conditional(clip, "-clip") +
    conditional(unc, "-unc") +
    required(inputWigFile) +
    required(inputChromSizesFile) +
    required(outputBigWig)
}