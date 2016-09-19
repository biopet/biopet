package nl.lumc.sasc.biopet.extensions.centrifuge

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by pjvanthof on 19/09/16.
 */
class CentrifugeKreport(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Output files centrifuge", required = true)
  var centrifugeOutputFiles: List[File] = Nil

  @Output(doc = "Output report")
  var output: File = _

  @Input(doc = "Centrifuge index prefix", required = true)
  var index: File = config("centrifuge_index", namespace = "centrifuge")

  executable = config("exe", default = "centrifuge-kreport", freeVar = false)

  def cmdLine = executable +
    //TODO: Options
    required("-x", index) +
    repeat(centrifugeOutputFiles)
}
