package nl.lumc.sasc.biopet.function.bedtools

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction

abstract class Bedtools extends BiopetCommandLineFunction {
  executable = config("exe", default = "bedtools", submodule = "bedtools")
  override def versionCommand = executable + " --version"
  override val versionRegex = """bedtools (.*)""".r
}