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
class CheckChecksum extends InProcessFunction {
  @Input(required = true)
  var inputFile: File = _

  @Input(required = true)
  var checksumFile: File = _

  @Argument(required = true)
  var checksum: String = _

  /** Exits whenever the input md5sum is not the same as the output md5sum */
  def run(): Unit = {
    val outputChecksum = WriteSummary.parseChecksum(checksumFile).toLowerCase

    if (outputChecksum != checksum.toLowerCase) {
      logger.error(s"Input file: '$inputFile' md5sum is not as expected, aborting pipeline")

      // 130 Simulates a ctr-C
      Runtime.getRuntime.halt(130)
    }
  }
}
