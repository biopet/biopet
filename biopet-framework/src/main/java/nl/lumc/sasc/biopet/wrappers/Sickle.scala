package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.io.Source._
import scala.sys.process._

class Sickle(private var globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  this.analysisName = "sickle"
  var config: Config = globalConfig.getAsConfig("sickle")
  
  @Input(doc="Sickle exe", required=false) var sickle_exe: File = new File("/usr/local/bin/sickle")
  @Input(doc="R1 input") var input_R1: File = null
  @Input(doc="R2 input", required=false) var input_R2: File = null
  @Output(doc="R1 output") var output_R1: File = null
  @Output(doc="R2 output", required=false) var output_R2: File = null
  @Output(doc="singles output", required=false) var output_singles: File = null
  @Output(doc="stats output") var output_stats: File = null

  @Input(doc="qualityType file", required=false) var qualityTypeFile: File = null
  var qualityType: String = config.getAsString("qualitytype", null)
  var defaultQualityType: String = config.getAsString("defaultqualitytype", "sanger")

  def commandLine = {
    this.addJobReportBinding("version", getVersion)
    this.getQualityTypeFromFile
    if (qualityType == null && defaultQualityType != null) qualityType = defaultQualityType
    var cmd: String = required(sickle_exe)
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
  
  def getQualityTypeFromFile {
    if (qualityType == null && qualityTypeFile != null) {
      if (qualityTypeFile.exists()) {
        for (line <- fromFile(qualityTypeFile).getLines) {
          var s: String = line.substring(0,line.lastIndexOf("\t"))
          qualityType = s
        }
      } else logger.warn("File : " + qualityTypeFile + " does not exist")
    }
  }
  
  private var version: String = _
  def getVersion : String = {
    val REG = """sickle version (.*)""".r
    if (version == null) for (line <- (sickle_exe + " --version").!!.split("\n")) {
      line match { 
        case REG(m) => {
            version = m
            return version
        }
        case _ =>
      }
    }
    return version
  }
}