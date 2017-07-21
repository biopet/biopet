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
package nl.lumc.sasc.biopet.pipelines.generateindexes

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by pjvanthof on 15/05/16.
  */
class WriteConfig extends InProcessFunction {
  @Input
  var deps: List[File] = Nil

  @Output(required = true)
  var out: File = _

  var config: Map[String, Any] = _

  def run(): Unit = {
    val writer = new PrintWriter(out)
    writer.println(ConfigUtils.mapToJson(config).spaces2)
    writer.close()
  }
}
