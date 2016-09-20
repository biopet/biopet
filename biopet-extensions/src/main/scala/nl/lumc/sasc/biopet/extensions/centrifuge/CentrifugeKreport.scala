package nl.lumc.sasc.biopet.extensions.centrifuge

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by pjvanthof on 19/09/16.
 */
class CentrifugeKreport(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Output files centrifuge", required = true)
  var centrifugeOutputFiles: List[File] = Nil

  @Output(doc = "Output report")
  var output: File = _

  @Input(doc = "Centrifuge index prefix", required = true)
  var index: File = config("centrifuge_index", namespace = "centrifuge")

  var onlyUnique: Boolean = config("only_unique", default = false)
  var showZeros: Boolean = config("show_zeros", default = false)
  var isCounts: Boolean = config("is_counts", default = false)

  var minScore: Option[Double] = config("min_score")
  var minLength: Option[Int] = config("min_length")

  executable = config("exe", default = "centrifuge-kreport", freeVar = false)

  def cmdLine = executable +
    conditional(onlyUnique, "--only-unique") +
    conditional(showZeros, "--show-zeros") +
    conditional(showZeros, "--is-counts") +
    optional("--min-score=", minScore, spaceSeparated = false) +
    optional("--min-length=", minLength, spaceSeparated = false) +
    required("-x", index) +
    repeat(centrifugeOutputFiles) +
    " > " + required(output)
}
