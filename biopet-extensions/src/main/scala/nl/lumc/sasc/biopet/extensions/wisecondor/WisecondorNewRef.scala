package nl.lumc.sasc.biopet.extensions.wisecondor

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by Sander Bollen on 22-3-17.
 */
class WisecondorNewRef(val parent: Configurable) extends Wisecondor {

  @Input
  var inputBeds: List[File] = Nil

  var nNeighbourBins: Option[Int] = config("n_neighbour_bins", namespace = "wisecondor", default = None)

  def cmdLine = executable +
    required("newref") +
    repeat("-I", inputBeds) +
    required("-O", output) +
    required("-R", referenceFasta()) +
    binCommand +
    optional("-n", nNeighbourBins)

}
