package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File

class Bwa(private var globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  this.analysisName = "bwa"
  var config: Config = globalConfig.getAsConfig("bwa")

  @Argument(doc="Bwa executeble", shortName="Bwa_Exe")
  var bwa_exe: String = config.getAsString("exe", "/usr/local/bin/bwa")
  @Input(doc="The reference file for the bam files.", shortName="R") var referenceFile: File = _
  @Input(doc="Fastq file R1", shortName="R1") var R1: File = _
  @Input(doc="Fastq file R2", shortName="R2", required=false) var R2: File = _
  @Output(doc="Output file SAM", shortName="output") var output: File = _
  
  @Argument(doc="Readgroup header", shortName="RG", required=false) var RG: String = _
  @Argument(doc="M", shortName="M", required=false) var M: Boolean = config.getAsBoolean("M", true)

  def commandLine = {
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
}