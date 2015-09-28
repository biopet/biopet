package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Output, Argument }

/**
 * Created by ahbbollen on 23-9-15.
 * This is python, but not accessed like a script; i.e. called by simply
 * manwe [subcommand]
 */
abstract class Manwe extends BiopetCommandLineFunction {
  executable = config("exe", default = "manwe", submodule = "manwe")

  override def defaultCoreMemory = 2.0
  override def defaultThreads = 1

  @Argument(doc = "Path to manwe config file containing your database settings", required = true)
  var manweConfig: Option[File] = config("manwe_config")

  @Output(doc = "the output file")
  var output: File = _

  var manweHelp: Boolean = false

  def subCommand: String

  final def cmdLine = {
    required(executable) +
      subCommand +
      required("-c", manweConfig) +
      conditional(manweHelp, "-h") +
      " > " +
      required(output)
  }

  /**
   * Convert cmdLine into line without quotes and double spaces
   * primarily for testing
   * @return
   */
  final def cmd = {
    val a = cmdLine
    a.replace("'", "").replace("  ", " ").trim
  }
}
