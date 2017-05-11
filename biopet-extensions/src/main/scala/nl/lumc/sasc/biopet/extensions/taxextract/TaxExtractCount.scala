package nl.lumc.sasc.biopet.extensions.taxextract

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
  * Created by Sander Bollen on 2-5-17.
  */
class TaxExtractCount(val parent: Configurable) extends TaxExtract {

  def subCommand = "count"

  @Output(required = false)
  var output: Option[File] = None

  @Argument(required = false)
  var reverse: Boolean = false

  override def cmdLine = {
    super.cmdLine +
      conditional(reverse, "--reverse") +
      (if (outputAsStdout) "" else " > " + required(output))
  }

}
