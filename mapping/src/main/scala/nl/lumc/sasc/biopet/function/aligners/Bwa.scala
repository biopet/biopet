package nl.lumc.sasc.biopet.function.aligners

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.sys.process._

class Bwa(val globalConfig: Config) extends BiopetCommandLineFunction {
  @Input(doc="Fastq file R1", shortName="R1")
  var R1: File = _
  
  @Input(doc="Fastq file R2", shortName="R2", required=false)
  var R2: File = _
  
  @Input(doc="The reference file for the bam files.", shortName="R")
  var referenceFile: File = new File(config.getAsString("referenceFile"))
  
  @Output(doc="Output file SAM", shortName="output")
  var output: File = _
  
  executeble = config.getAsString("exe", "bwa")
  
  var RG: String = _
  var M = config.getAsBoolean("M", true)
  
  override val defaultVmem = "6G"
  override val defaultThreads = 8
  override val versionRegex = """Version: (.*)""".r
  
  override def beforeCmd() {
    versionCommand = executeble
  }
  
  def cmdLine = {
    required(executeble) + 
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
