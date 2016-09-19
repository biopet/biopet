package nl.lumc.sasc.biopet.extensions.centrifuge

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Version }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.util.matching.Regex

/**
 * Created by pjvanthof on 19/09/16.
 */
class Centrifuge(val root: Configurable) extends BiopetCommandLineFunction with Version {
  @Input(doc = "Input: FastQ or FastA", required = true)
  var inputR1: File = _

  @Input(doc = "Input: FastQ or FastA", required = false)
  var inputR2: Option[File] = None

  @Input(doc = "Centrifuge index prefix", required = true)
  var index: File = config("centrifige_index")

  @Output(doc = "Output with hits per sequence")
  var output: File = _

  @Output(doc = "Output with hits per sequence")
  var report: Option[File] = None

  override def defaultThreads = 8

  executable = config("exe", default = "centrifuge", freeVar = false)

  /** Command to get version of executable */
  def versionCommand: String = s"$executable --version"

  /** Regex to get version from version command output */
  def versionRegex: Regex = ".* version (.*)".r

  /**
   * This function needs to be implemented to define the command that is executed
   *
   * @return Command to run
   */
  def cmdLine: String = executable +
    //TODO: Options
    optional("--threads", threads)
  required("-x", index) +
    (inputR2 match {
      case Some(r2) => required("-1", inputR1) + required("-2", r2)
      case _        => required("-U", inputR1)
    }) +
    (if (outputAsStsout) "" else required("-S", output)) +
    optional("--report-file", report)
}
