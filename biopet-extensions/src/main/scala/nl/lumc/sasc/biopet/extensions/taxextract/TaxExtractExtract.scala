package nl.lumc.sasc.biopet.extensions.taxextract

import java.io.File

import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * Created by Sander Bollen on 2-5-17.
  */
class TaxExtractExtract(val parent: Configurable) extends TaxExtract {

  def subCommand = "extract"

  @Input
  var centrifugeResult: File = _

  @Input
  var fq1 : File = _

  @Input(required = false)
  var fq2: Option[File] = None

  @Output
  var out1 : File = _

  @Output(required = false)
  var out2: Option[File] = None

  @Argument(required = false)
  var noChildren: Boolean = false

  @Argument(required = false)
  var reverse: Boolean = false

  override def cmdLine = {
    if (List(fq2, out2).count(_.isDefined) == 1) {
      Logging.addError("Both fq2 and out2 must be defined if either one is defined")
    }
    super.cmdLine +
      required("-r", centrifugeResult) +
      required("-fq1", fq1) +
      required("-o1", out1) +
      optional("-fq2", fq2) +
      optional("-o2", out2) + conditional(noChildren, "--no-children") +
      conditional(reverse, "--reverse")
  }

}
