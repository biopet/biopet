package nl.lumc.sasc.biopet.extensions.samtools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
 * Created by pjvanthof on 22/09/15.
 */
class SamtoolsSort(val root: Configurable) extends Samtools {

  @Input(required = true)
  var input: File = _

  @Output
  var output: File = _

  var compresion: Option[Int] = config("l")
  var outputFormat: Option[String] = config("O")
  var sortByName: Boolean = config("sort_by_name", default = false)
  var prefix: String = _

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    prefix = config("prefix", default = new File(System.getProperty("java.io.tmpdir"), output.getName))
  }

  def cmdLine = required(executable) + required("sort") +
    optional("-m", (coreMemeory + "G")) +
    optional("-@", threads) +
    optional("-O", outputFormat) +
    required("-T", prefix) +
    conditional(sortByName, "-n") +
    (if (outputAsStsout) "" else required("-o", output)) +
    (if (inputAsStdin) "" else required(input))
}
