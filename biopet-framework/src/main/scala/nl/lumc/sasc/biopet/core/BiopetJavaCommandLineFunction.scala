package nl.lumc.sasc.biopet.core

import org.broadinstitute.sting.queue.function.JavaCommandLineFunction

abstract class BiopetJavaCommandLineFunction extends JavaCommandLineFunction with BiopetCommandLineFunctionTrait {
  executeble = "java"
  
  override def commandLine: String = {
    preCmdInternal
    val cmd = super.commandLine
    val finalCmd = executeble + cmd.substring(cmd.indexOf(" "))
//    addJobReportBinding("command", cmd)
    return cmd
  }
}
