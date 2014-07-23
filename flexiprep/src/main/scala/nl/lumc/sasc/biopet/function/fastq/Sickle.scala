package nl.lumc.sasc.biopet.function.fastq

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File
import scala.io.Source._
import scala.sys.process._

class Sickle(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "R1 input")
  var input_R1: File = null

  @Input(doc = "R2 input", required = false)
  var input_R2: File = null

  @Input(doc = "qualityType file", required = false)
  var qualityTypeFile: File = _

  @Output(doc = "R1 output")
  var output_R1: File = null

  @Output(doc = "R2 output", required = false)
  var output_R2: File = null

  @Output(doc = "singles output", required = false)
  var output_singles: File = null

  @Output(doc = "stats output")
  var output_stats: File = null

  executable = config("exe", default = "sickle")
  var qualityType: String = config("qualitytype")

  var defaultQualityType: String = _
  override val versionRegex = """sickle version (.*)""".r

  override def afterGraph {
    if (defaultQualityType == null) defaultQualityType = config("defaultqualitytype", default = "sanger")
    if (qualityType == null && defaultQualityType != null) qualityType = defaultQualityType
  }

  override def versionCommand = executable + " --version"

  override def beforeCmd {
    qualityType = getQualityTypeFromFile
  }

  def cmdLine = {
    var cmd: String = required(executable)
    if (input_R2 != null) {
      cmd += required("pe") +
        required("-r", input_R2) +
        required("-p", output_R2) +
        required("-s", output_singles)
    } else cmd += required("se")
    cmd +
      required("-f", input_R1) +
      required("-f", input_R1) +
      required("-t", qualityType) +
      required("-o", output_R1) +
      " > " + required(output_stats)
  }

  def getQualityTypeFromFile: String = {
    if (qualityType == null && qualityTypeFile != null) {
      if (qualityTypeFile.exists()) {
        for (line <- fromFile(qualityTypeFile).getLines) {
          var s: String = line.substring(0, line.lastIndexOf("\t"))
          return s
        }
      } else logger.warn("File : " + qualityTypeFile + " does not exist")
    }
    return null
  }
}