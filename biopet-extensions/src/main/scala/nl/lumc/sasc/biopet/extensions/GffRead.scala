package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Version }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by pjvanthof on 20/06/16.
 */
class GffRead(val root: Configurable) extends BiopetCommandLineFunction {

  executable = config("exe", default = "gffread", freeVar = false)

  @Input
  var input: File = _

  @Output
  var output: File = _

  var T: Boolean = config("T", default = false, freeVar = false)

  def cmdLine = executable +
    (if (inputAsStdin) "" else required(input)) +
    (if (outputAsStsout) "" else required("-o", output)) +
    conditional(T, "-T")
}
