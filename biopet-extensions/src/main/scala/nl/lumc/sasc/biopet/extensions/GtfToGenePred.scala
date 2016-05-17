package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by pjvan_thof on 17-5-16.
 */
class GtfToGenePred(val root: Configurable) extends BiopetCommandLineFunction {
  executable = config("exe", default = "gtfToGenePred", freeVar = false)

  @Input
  var inputGtfs: List[File] = Nil

  @Output
  var outputGenePred: File = _

  @Output
  var infoOut: Option[File] = None

  var genePredExt: Boolean = config("gene _pred _ext", default = false)
  var allErrors: Boolean = config("all_errors", default = false)
  var impliedStopAfterCds: Boolean = config("implied_stop_after_cds", default = false)
  var simple: Boolean = config("simple", default = false)
  var geneNameAsName2: Boolean = config("gene _name_as_name2", default = false)

  def cmdLine = executable +
    conditional(genePredExt, "-genePredExt") +
    conditional(allErrors, "-allErrors") +
    optional("-infoOut", infoOut) +
    conditional(allErrors, "-allErrors") +
    conditional(impliedStopAfterCds, "-impliedStopAfterCds") +
    conditional(simple, "-simple") +
    conditional(geneNameAsName2, "-geneNameAsName2") +
    repeat(inputGtfs) +
    (if (outputAsStsout) required("/dev/stdout") else required(outputGenePred))
}
