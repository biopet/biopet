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
package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.File

import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.Input

import scala.io.Source

/**
 * This class checks results of [[nl.lumc.sasc.biopet.tools.ValidateVcf]] and aborts the pipeline when a error was been found
 *
 * Created by pjvanthof on 16/08/15.
 */
class CheckValidateVcf extends InProcessFunction {
  @Input(required = true)
  var inputLogFile: File = _

  /** Exits whenever the input md5sum is not the same as the output md5sum */
  def run: Unit = {

    val reader = Source.fromFile(inputLogFile)
    reader.getLines().foreach { line =>
      if (line.startsWith("ERROR")) {
        logger.error("Corrupt vcf file found, aborting pipeline")

        // 130 Simulates a ctr-C
        Runtime.getRuntime.halt(130)
      }
    }
    reader.close()
  }
}