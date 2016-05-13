package nl.lumc.sasc.biopet.pipelines.generateindexes

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by pjvan_thof on 13-5-16.
 */
class FastaMerging(val root: Configurable) extends BiopetCommandLineFunction {
  @Input
  var input: List[File] = Nil

  @Output(required = true)
  var output: File = _

  var cmds: Array[BiopetCommandLineFunction] = Array()

  def cmdLine = cmds.mkString(" && ")

}
