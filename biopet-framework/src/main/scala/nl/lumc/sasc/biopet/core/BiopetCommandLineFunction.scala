package nl.lumc.sasc.biopet.core

abstract class BiopetCommandLineFunction extends BiopetCommandLineFunctionTrait {
  protected def cmdLine: String
  final def commandLine: String = {
    preCmdInternal
    val cmd = cmdLine
    addJobReportBinding("command", cmd)
    return cmd
  }
}
