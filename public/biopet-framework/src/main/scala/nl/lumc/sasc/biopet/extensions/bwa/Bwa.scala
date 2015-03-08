package nl.lumc.sasc.biopet.extensions.bwa

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction

/**
 * General bwa extension
 *
 * Created by pjvan_thof on 1/16/15.
 */
abstract class Bwa extends BiopetCommandLineFunction {
  override def subPath = "bwa" :: super.subPath
  executable = config("exe", default = "bwa")
  override val versionRegex = """Version: (.*)""".r
  override val versionExitcode = List(0, 1)
  override def versionCommand = executable
}
