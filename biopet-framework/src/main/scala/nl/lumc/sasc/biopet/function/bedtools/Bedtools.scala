package nl.lumc.sasc.biopet.function.bedtools

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction

abstract class Bedtools extends BiopetCommandLineFunction {
  executeble = config("exe", "bedtools", "bedtools")
  override def versionCommand = executeble + " --version"
  override val versionRegex = """bedtools (.*)""".r
}