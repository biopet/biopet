package nl.lumc.sasc.biopet.extensions.bedtools

import java.io.File

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Created by Sander Bollen on 26-5-16.
 */
class BedtoolsSort(val root: Configurable) extends Bedtools with Reference {

  @Input
  var input: File = null

  @Output
  var output: File = null

  @Argument(required = false)
  var faidx: File = referenceFai

  def cmdLine = required(executable) + required("sort") + required("-i", input) +
    optional("-faidx", faidx) +
    (if (outputAsStsout) "" else " > " + required(output))

}
