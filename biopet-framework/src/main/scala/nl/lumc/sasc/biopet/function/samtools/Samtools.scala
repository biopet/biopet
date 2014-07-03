package nl.lumc.sasc.biopet.function.samtools

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction

abstract class Samtools extends BiopetCommandLineFunction {
  executeble = config("exe", "samtools", "samtools")
  override def versionCommand = executeble
  override val versionRegex = """Version: (.*)""".r
  override val versionExitcode = List(0,1)
}