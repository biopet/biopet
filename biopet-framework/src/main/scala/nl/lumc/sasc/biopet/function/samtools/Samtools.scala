package nl.lumc.sasc.biopet.function.samtools

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction

abstract class Samtools extends BiopetCommandLineFunction {
  executable = config("exe", default = "samtools", submodule = "samtools")
  override def versionCommand = executable
  override val versionRegex = """Version: (.*)""".r
  override val versionExitcode = List(0, 1)
}