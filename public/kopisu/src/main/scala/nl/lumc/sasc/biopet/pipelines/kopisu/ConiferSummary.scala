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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.kopisu

import java.io.{ FileWriter, BufferedWriter, File, PrintWriter }

import argonaut._
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.io.Source

class ConiferSummary(val root: Configurable) extends InProcessFunction with Configurable {
  def filterCalls(callFile: File, outFile: File, sampleName: String): Unit = {
    //    val filename = callFile.getAbsolutePath
    val writer = new BufferedWriter(new FileWriter(outFile))

    for (line <- Source.fromFile(callFile).getLines()) {
      line.startsWith(sampleName) || line.startsWith("sampleID") match {
        case true => writer.write(line)
        case _    =>
      }
    }
  }

  this.analysisName = getClass.getSimpleName

  @Input(doc = "deps")
  var deps: List[File] = Nil

  @Output(doc = "Summary output", required = true)
  var out: File = _

  @Input(doc = "calls")
  var calls: File = _

  var label: String = _

  var coniferPipeline: ConiferPipeline = if (root.isInstanceOf[ConiferPipeline]) root.asInstanceOf[ConiferPipeline] else {
    throw new IllegalStateException("Root is no instance of ConiferPipeline")
  }

  var resources: Map[String, Json] = Map()

  override def run {
    logger.debug("Start")
    filterCalls(calls, out, label)
    logger.debug("Stop")
  }
}
