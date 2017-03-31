package nl.lumc.sasc.biopet.extensions.wisecondor

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by Sander Bollen on 20-3-17.
 */
class WisecondorCount(val parent: Configurable) extends Wisecondor {

  @Input
  var inputBam: File = _

  def cmdLine = executable +
    required("count") +
    required("-I", inputBam) +
    required("-O", output) +
    required("-R", referenceFasta()) +
    binCommand
}
