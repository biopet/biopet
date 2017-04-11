package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import java.util

import org.broadinstitute.gatk.engine.recalibration.BQSRGatherer
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

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
