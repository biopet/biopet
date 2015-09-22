package nl.lumc.sasc.biopet.extensions.samtools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Output, Input}

/**
 * Created by pjvanthof on 22/09/15.
 */
class SamtoolsSort(val root: Configurable) extends Samtools {

  @Input(required = true)
  var input: File = _

  @Output
  var output: File = _

  val compresion: Option[Int] = config("l")
  val outputFormat: Option[String] = config("O")
  val sortByName: Boolean = config("sort_by_name", default = false)
  val prefix: String = config("prefix", default = new File(qSettings.tempDirectory, output.getAbsolutePath))

  def cmdLine = optional("-m", (coreMemeory + "G")) +
    optional("-@", threads) +
    optional("-O", outputFormat) +
    conditional(sortByName, "-n") +
    (if (outputAsStsout) "" else  required("-o", output)) +
    (if (inputAsStdin) "" else required(input))
}
