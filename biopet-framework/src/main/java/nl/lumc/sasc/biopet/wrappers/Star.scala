package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.sys.process._

class Star(private var globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  this.analysisName = "STAR"
  var config: Config = Config.mergeConfigs(globalConfig.getAsConfig("star"), globalConfig)

  @Argument(doc="STAR executeble", shortName="star_exe", required=false)
  var star_exe: String = config.getAsString("exe", "/usr/local/bin/STAR")
  @Input(doc="The reference file for the bam files.", shortName="R") var referenceFile: File = new File(config.getAsString("referenceFile"))
  @Input(doc="Fastq file R1", shortName="R1") var R1: File = _
  @Input(doc="Fastq file R2", shortName="R2", required=false) var R2: File = _
  @Argument(doc="Output Directory", shortName="outputDir") var outputDir: String = _
  @Argument(doc="GenomeDir", required=false) var genomeDir: String = _
  if (genomeDir == null) genomeDir = config.getAsString("genomeDir", referenceFile.getParent + "/star")
  
  @Argument(doc="STAR runmode", shortName="runmode", required=false) var runmode: String = _
  
  this.addJobReportBinding("version", "NA")

  jobResourceRequests :+= "h_vmem=" + config.getAsString("vmem", "6G")
  var threads: Int = config.getAsInt("threads", 8)
  var maxThreads: Int = config.getAsInt("maxthreads", 24)
  if (threads > maxThreads) threads = maxThreads
  nCoresRequest = Option(threads)

  @Output var outputSam: File = new File(outputDir + "/Aligned.out.sam")
  
  def commandLine : String= {
    //init()
    var cmd: String = required("cd",outputDir) + "&&" + required(star_exe)
    if (runmode != null && runmode == "genomeGenerate") { // Create index
      cmd += required("--runmode", runmode) +
        required("--genomeDir", genomeDir) +
        required("--genomeFastaFiles", referenceFile)
    } else { // Aligner
      cmd += required("--genomeDir", genomeDir) +
        required("--readFilesIn", R1) + optional(R2)
    }
    cmd += optional("--runThreadN", nCoresRequest)
    
    return cmd
  }
}