package nl.lumc.sasc.biopet.wrappers.fastq

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.sys.process._

class Fastqc(val globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  this.analysisName = "fastqc"
  val config: Config = Config.mergeConfigs(globalConfig.getAsConfig(analysisName), globalConfig)
  logger.debug("Config for " + this.analysisName + ": " + config)
  
  @Input(doc="fastqc executeble", shortName="Fastqc_Exe")
  var fastqc_exe: File = new File(config.getAsString("exe","/usr/local/FastQC/FastQC_v0.10.1/fastqc"))
  @Argument(doc="java vm executeble", shortName="Java_Exe", required=false)
  var java_exe: String = globalConfig.getAsConfig("java").getAsString("exe", "java")
  @Argument(doc="kmers", required=false) var kmers: Int = config.getAsInt("kmers", 5)
  @Argument(doc="threads", required=false) var threads: Int = config.getAsInt("threads", 4)
  @Argument(doc="quiet", required=false) var quiet: Boolean = config.getAsBoolean("quiet", false)
  @Argument(doc="noextract", required=false) var noextract: Boolean = config.getAsBoolean("noextract", false)
  @Argument(doc="nogroup", required=false) var nogroup: Boolean = config.getAsBoolean("nogroup", false)
  @Input(doc="Contaminants", required=false)
  var contaminants: File = new File(config.getAsString("contaminants",fastqc_exe.getParent() + "/Contaminants/contaminant_list.txt"))
  @Input(doc="Fastq file", shortName="FQ") var fastqfile: File = _
  @Output(doc="Output", shortName="out") var output: File = _
  
  if (config.contains("vmem")) jobResourceRequests :+= "h_vmem=" + config.getAsString("vmem")
    
  def init() {
    this.addJobReportBinding("version", getVersion)
    var maxThreads: Int = config.getAsInt("maxthreads", 24)
    if (threads > maxThreads) threads = maxThreads
    nCoresRequest = Option(threads)
    this.jobNativeArgs :+= "-l h_vmem="+config.getAsString("vmem", "4G")
  }
  
  def commandLine = {
    init()
    required(fastqc_exe) + 
      optional("--java", java_exe) +
      optional("--threads",threads) +
      optional("--contaminants",contaminants) +
      optional("--kmers",kmers) +
      conditional(nogroup, "--nogroup") +
      conditional(noextract, "--noextract") +
      conditional(quiet, "--quiet") +
      required("-o",output.getParent()) + 
      required(fastqfile) + 
      required(" > ", output, escape=false)
  }
  
  private var version: String = _
  var versionCommand = fastqc_exe + " --version"
  var versionRegex = """FastQC (.*)"""
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