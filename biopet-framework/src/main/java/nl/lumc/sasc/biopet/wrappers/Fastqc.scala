package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.sys.process._

class Fastqc(private var globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  this.analysisName = "fastqc"
  var config: Config = globalConfig.getAsConfig("fastqc")
  
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
  
  def commandLine = {
    this.addJobReportBinding("version", getVersion)
    if (config.contains("fastqc_exe")) fastqc_exe = new File(config.get("fastqc_exe").toString)
    this.nCoresRequest = Option(threads)
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
  def getVersion : String = {
    val REG = """FastQC (.*)""".r
    if (version == null) for (line <- (fastqc_exe + " --version").!!.split("\n")) {
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