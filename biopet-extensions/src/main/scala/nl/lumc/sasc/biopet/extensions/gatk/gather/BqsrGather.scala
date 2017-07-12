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
package nl.lumc.sasc.biopet.extensions.gatk.gather

import java.io.File
import java.util

import org.broadinstitute.gatk.engine.recalibration.BQSRGatherer
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
 * Created by pjvanthof on 05/04/2017.
 */
class BqsrGather extends InProcessFunction {

  @Input(required = true)
  var inputBqsrFiles: List[File] = _

  @Output(required = true)
  var outputBqsrFile: File = _

  def run(): Unit = {
    val l = new util.ArrayList[File]()
    inputBqsrFiles.foreach(l.add(_))
    val gather = new BQSRGatherer
    gather.gather(l, outputBqsrFile)
  }
}
