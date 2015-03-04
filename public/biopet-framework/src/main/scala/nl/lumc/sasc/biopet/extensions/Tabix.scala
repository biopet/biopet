package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Output, Input}

/**
 * Created by pjvan_thof on 3/4/15.
 */
class Tabix(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Input files", required = true)
  var input: File = null

  @Output(doc = "Compressed output file", required = true)
  protected var output: File = null

  var p: Option[String] = config("p")
  executable = config("exe", default = "bgzip")

  override def beforeGraph: Unit = {
    output = new File(input.getAbsolutePath + ".tbi")
  }

  def cmdLine = required(executable) +
    optional("-p", p) +
    required(input)
}