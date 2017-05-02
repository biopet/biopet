package nl.lumc.sasc.biopet.extensions.taxextract

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Output}

/**
  * Created by Sander Bollen on 2-5-17.
  */
class TaxExtractId(val parent: Configurable) extends TaxExtract {

  def subCommand = "tax-id"

  @Output(required = false)
  var output: Option[File] = None

  @Argument(required = false)
  var noChildren: Boolean = false

  @Argument(required = false)
  var reverse: Boolean = false

  override def cmdLine: String = {
    super.cmdLine +
      conditional(noChildren, "--no-children") +
      conditional(reverse, "--reverse") +
      (if (outputAsStsout) "" else " > " + required(output))
  }

}
