package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by pjvan_thof on 9-5-17.
  */
class FakeJob(val parent: Configurable) extends BiopetCommandLineFunction {

  @Input
  var inputFiles: List[File] = Nil

  @Output
  var outputFiles: List[File] = Nil

  /**
    * This function needs to be implemented to define the command that is executed
    *
    * @return Command to run
    */
  def cmdLine: String = ""
}
