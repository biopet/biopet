package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.sys.process._

class Bwa(val globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  this.analysisName = "bwa"
  var config: Config = _

  @Argument(doc="Bwa executeble", shortName="bwa_exe", required=false) var bwa_exe: String = _
  @Input(doc="The reference file for the bam files.", shortName="R", required=false) var referenceFile: File = _
  @Input(doc="Fastq file R1", shortName="R1") var R1: File = _
  @Input(doc="Fastq file R2", shortName="R2", required=false) var R2: File = _
  @Output(doc="Output file SAM", shortName="output") var output: File = _
  
  @Argument(doc="Readgroup header", shortName="RG", required=false) var RG: String = _
  @Argument(doc="M", shortName="M", required=false) var M: Boolean = false
  
  def init() {
    config = Config.mergeConfigs(globalConfig.getAsConfig("bwa"), globalConfig)
    bwa_exe = config.getAsString("exe", "/usr/local/bin/bwa")
    M = config.getAsBoolean("M", true)
    jobResourceRequests :+= "h_vmem=" + config.getAsString("vmem", "6G")
    var threads: Int = config.getAsInt("threads", 8)
    var maxThreads: Int = config.getAsInt("maxthreads", 24)
    if (threads > maxThreads) threads = maxThreads
    nCoresRequest = Option(threads)
    
    referenceFile = new File(config.getAsString("referenceFile"))
    
    this.addJobReportBinding("version", getVersion)
  }
  
  def commandLine = {
    init()
    required(bwa_exe) + 
    required("mem") + 
    optional("-t", nCoresRequest) + 
    optional("-R", RG) + 
    conditional(M, "-M") +
    required(referenceFile) + 
    required(R1) + 
    optional(R2) + 
    " > " + required(output)
  }
  
  private var version: String = ""
  def getVersion : String = {
    val REG = """Version: (.*)""".r
    if (version == null) for (line <- bwa_exe.!!.split("\n")) {
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
