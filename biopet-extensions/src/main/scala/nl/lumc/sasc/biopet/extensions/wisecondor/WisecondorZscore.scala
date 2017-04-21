package nl.lumc.sasc.biopet.extensions.wisecondor

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by Sander Bollen on 20-3-17.
 */
class WisecondorZscore(val parent: Configurable) extends Wisecondor {

  @Input
  var inputBed: File = _

  @Input
  var referenceDictionary: File = _

  def cmdLine = executable +
    required("zscore") +
    required("-I", inputBed) +
    required("-O", output) +
    required("-R", referenceFasta()) +
    required("-D", referenceDictionary) +
    binCommand
}
