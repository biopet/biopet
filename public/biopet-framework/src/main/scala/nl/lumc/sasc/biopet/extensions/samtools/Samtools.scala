package nl.lumc.sasc.biopet.extensions.samtools

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction

abstract class Samtools extends BiopetCommandLineFunction {
  executable = config("exe", default = "samtools", submodule = "samtools", freeVar = false)
  override def versionCommand = executable
  override val versionRegex = """Version: (.*)""".r
  override val versionExitcode = List(0, 1)
}