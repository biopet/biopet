package nl.lumc.sasc.biopet.extensions.macs2

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction

/**
 * General igvtools extension
 *
 * Created by sajvanderzeeuw on 12/19/14.
 */
abstract class Macs2 extends BiopetCommandLineFunction {
  executable = config("exe", default = "macs2", submodule = "macs2", freeVar = false)
  override def versionCommand = executable + " --version"
  override val versionRegex = """macs2 (.*)""".r
  override val versionExitcode = List(0, 1)
}
