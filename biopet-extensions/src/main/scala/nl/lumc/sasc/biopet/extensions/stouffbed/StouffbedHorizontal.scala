package nl.lumc.sasc.biopet.extensions.stouffbed

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Output

/**
 * Created by Sander Bollen on 24-4-17.
 */
class StouffbedHorizontal(val parent: Configurable) extends Stouffbed {

  @Output
  var output: File = _

  def cmdLine: String = {
    executable +
      required("horizontal") +
      repeat("-i", inputs) +
      required("-o", output)
  }

}
