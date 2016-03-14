package nl.lumc.sasc.biopet.extensions.qiime

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 12/4/15.
 */
class PickRepSet(val root: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "pick_rep_set.py")

  @Input(required = true)
  var inputFile: File = _

  @Output
  var outputFasta: Option[File] = None

  @Output
  var logFile: Option[File] = None

  @Input(required = false)
  var referenceSeqsFp: Option[File] = config("reference_seqs_fp")

  @Input(required = false)
  var fastaInput: Option[File] = None

  var sortBy: Option[String] = config("sort_by")

  def versionCommand = executable + " --version"
  def versionRegex = """Version: (.*)""".r

  var repSetPickingMethod: Option[String] = config("rep_set_picking_method")

  def cmdLine = executable +
    required("-i", inputFile) +
    required("-o", outputFasta) +
    optional("-m", repSetPickingMethod) +
    optional("-f", fastaInput) +
    optional("-l", logFile) +
    optional("-s", sortBy) +
    optional("-r", referenceSeqsFp)
}
