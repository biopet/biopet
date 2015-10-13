package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFuntion
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Output, Input}

/**
 * Created by ahbbollen on 13-10-15.
 */
class GvcfToBed (val root: Configurable) extends ToolCommandFuntion {
  def toolObject = nl.lumc.sasc.biopet.tools.GvcfToBed

  @Input(doc = "input vcf")
  var inputVcf: File = _

  @Output(doc = "output bed")
  var outputBed: File = _

  @Argument(doc = "sample")
  var sample: Option[String] = None

  @Argument(doc = "minquality")
  var minQuality: Int = 0

  @Argument(doc = "inverse")
  var inverse: Boolean = false

  override def defaultCoreMemory = 4.0

  override def cmdLine = {
    super.cmdLine +
    required("-I", inputVcf) +
    required("-O", outputBed) +
    optional("-S", sample) +
    optional("--minGenomeQuality", minQuality) +
    conditional(inverse, "--inverted")
  }

}
