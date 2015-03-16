/**
 * Created by wyleung on 5-1-15.
 */

package nl.lumc.sasc.biopet.extensions.igvtools

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction

/** General igvtools extension */
abstract class IGVTools extends BiopetCommandLineFunction {
  executable = config("exe", default = "igvtools", submodule = "igvtools", freeVar = false)
  override def versionCommand = executable + " version"
  override val versionRegex = """IGV Version:? ([\w\.]*) .*""".r
  override val versionExitcode = List(0)
}