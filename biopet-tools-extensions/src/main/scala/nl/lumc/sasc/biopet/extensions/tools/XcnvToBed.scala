package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * Created by Sander Bollen on 24-11-16.
  */
class XcnvToBed(val root: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.XcnvToBed

  @Input
  var inputXcnv: File = _

  @Output
  var outpuBed: File = _

  @Argument
  var sample: String = _

  override def cmdLine = {
    super.cmdLine + required("-I", inputXcnv) + required("-O", outpuBed) + required("-S", sample)
  }

}
