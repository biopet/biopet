package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.io.Source._
import scala.sys.process._

class Cutadapt(val globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  analysisName = "cutadapt"
  val config: Config = Config.mergeConfigs(globalConfig.getAsConfig("cutadapt"), globalConfig)
  logger.debug("Config for " + this.analysisName + ": " + config)
  
  @Input(doc="Cutadapt exe", required=false)
  var cutadapt_exe: File = new File(config.getAsString("exe","/usr/local/bin/cutadapt"))
  @Input(doc="Input fastq file") var fastq_input: File = _ 
  @Input(doc="Fastq contams file", required=false) var contams_file: File = _
  @Output(doc="Output fastq file") var fastq_output: File = _
  
  var default_clip_mode = config.getAsString("default_clip_mode", "3")
  var opt_adapter: Set[String] = config.getAsListOfStrings("adapter", Nil).to[Set]
  var opt_anywhere: Set[String] = config.getAsListOfStrings("anywhere", Nil).to[Set]
  var opt_front: Set[String] = config.getAsListOfStrings("front", Nil).to[Set]
  
  var opt_discard: Boolean = config.getAsBoolean("discard",false)
  var opt_minimum_length: String = config.getAsInt("minimum_length", 1).toString
  var opt_maximum_length: String = config.getAsString("maximum_length", null) 
  
  def init() {
    this.addJobReportBinding("version", getVersion)
    this.getContamsFromFile
  }
  
  def commandLine = {
    init()
    if (!opt_adapter.isEmpty || !opt_anywhere.isEmpty || !opt_front.isEmpty) {
      required(cutadapt_exe) +
      // options
      repeat("-a", opt_adapter) + 
      repeat("-b", opt_anywhere) + 
      repeat("-g", opt_front) + 
      conditional(opt_discard, "--discard") +
      optional("-m", opt_minimum_length) + 
      optional("-M", opt_maximum_length) + 
      // input / output
      required(fastq_input) +
      " > " + required(fastq_output)
    } else {
      "ln -sf " + 
      required(fastq_input) +
      required(fastq_output)
    }
  }
  
  def getContamsFromFile {
    if (contams_file != null) {
      if (contams_file.exists()) {
        for (line <- fromFile(contams_file).getLines) {
          var s: String = line.substring(line.lastIndexOf("\t")+1, line.size)
          if (default_clip_mode == "3") opt_adapter += s
          else if (default_clip_mode == "5") opt_front += s
          else if (default_clip_mode == "both") opt_anywhere += s
          else {
            opt_adapter += s
            logger.warn("Option default_clip_mode should be '3', '5' or 'both', falling back to default: '3'")
          }
          logger.info("Adapter: " + s + " found in: " + fastq_input)
        }
      } else logger.warn("File : " + contams_file + " does not exist")
    }
  }
  
  private var version: String = _
  var versionCommand = cutadapt_exe + " --version"
  var versionRegex = """(.*)"""
  def getVersion: String = getVersion(versionCommand, versionRegex)
  def getVersion(cmd:String, regex:String) : String = {
    val REG = regex.r
    if (cmd.! != 0) {
      logger.warn("Version command: '" + cmd + "' give a none-zero exit code, version not found")
      return "NA"
    }
    for (line <- cmd.!!.split("\n")) {
      line match { 
        case REG(m) => return m
        case _ =>
      }
    }
    logger.warn("Version command: '" + cmd + "' give a exit code 0 but no version was found, executeble oke?")
    return "NA"
  }
}