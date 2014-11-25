package nl.lumc.sasc.biopet.extensions.sambamba

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction

abstract class Sambamba extends BiopetCommandLineFunction {
  override val defaultVmem = "4G"
  override val defaultThreads = 2

  executable = config("exe", default = "sambamba", submodule = "sambamba", freeVar = false)
  override def versionCommand = executable
  override val versionRegex = """sambamba v(.*)""".r
  override val versionExitcode = List(0, 1)
}