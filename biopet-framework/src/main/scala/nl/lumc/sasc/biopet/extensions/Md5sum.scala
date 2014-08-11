package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline._
import java.io.File
import argonaut._, Argonaut._
import scalaz._, Scalaz._
import scala.io.Source

class Md5sum(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Input")
  var input: File = _

  @Output(doc = "Output")
  var output: File = _

  executable = config("exe", default = "md5sum")

  def cmdLine = required(executable) + required(input) + " > " + required(output)

  def getSummary: Json = {
    val data = Source.fromFile(output).mkString.split(" ")
    return ("path" := output.getAbsolutePath) ->:
      ("md5sum" := data(0)) ->:
      jEmptyObject
  }
}

object Md5sum {
  def apply(root: Configurable, fastqfile: File, outDir: String): Md5sum = {
    val md5sum = new Md5sum(root)
    md5sum.input = fastqfile
    md5sum.output = new File(outDir + fastqfile.getName + ".md5")
    return md5sum
  }
}
