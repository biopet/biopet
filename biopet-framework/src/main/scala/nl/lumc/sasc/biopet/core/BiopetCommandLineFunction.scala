package nl.lumc.sasc.biopet.core

import org.broadinstitute.gatk.utils.commandline._
import scala.sys.process._
import nl.lumc.sasc.biopet.core.config._

abstract class BiopetCommandLineFunction extends BiopetCommandLineFunctionTrait {  
  protected def cmdLine: String
  final def commandLine: String = {
    preCmdInternal
    val cmd = cmdLine
    addJobReportBinding("command", cmd)
    return cmd
  }
}
