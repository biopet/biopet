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

  def run: Unit = {
    val writer = new PrintWriter(out)
    writer.println(ConfigUtils.mapToJson(config).spaces2)
    writer.close()
  }
}