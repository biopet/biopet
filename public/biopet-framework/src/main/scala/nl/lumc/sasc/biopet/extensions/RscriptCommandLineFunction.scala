package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by wyleung on 17-2-15.
 */
trait RscriptCommandLineFunction  extends BiopetCommandLineFunction{
  @Input(doc = "R script", required = false)
  var script: File = _
  protected var scriptName: String = _

  executable = config("exe", default = "Rscript", submodule = "R")


  /**
   * Set the Rscript to run
   *
   * @param filename RScript file location
   */
  def setScript(filename: File) = {


  }

  def setScript(filename: File, subpackage: String) = {


  }

}
