package nl.lumc.sasc.biopet.wrappers.aligners

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.sys.process._

class Star(val globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  this.analysisName = "star"
  val config: Config = Config.mergeConfigs(globalConfig.getAsConfig(analysisName), globalConfig)
  logger.debug("Config for " + this.analysisName + ": " + config)
  
  @Argument(doc="STAR executeble", required=false) var star_exe: String = config.getAsString("exe", "/usr/local/bin/STAR")
  @Input(doc="The reference file for the bam files.", required=false) var referenceFile: File = new File(config.getAsString("referenceFile"))
  @Input(doc="Fastq file R1", required=false) var R1: File = _
  @Input(doc="Fastq file R2", required=false) var R2: File = _
  @Argument(doc="Output Directory") var outputDir: String = _
  @Argument(doc="GenomeDir", required=false) var genomeDir: String = config.getAsString("genomeDir", referenceFile.getParent + "/star/")
  @Argument(doc="STAR runmode", shortName="runmode", required=false) var runmode: String = _
  @Output(doc="Output SAM file", required=false) var outputSam: File = _
  @Output(doc="Output tab file", required=false) var outputTab: File = _
  @Input(doc="sjdbFileChrStartEnd file", required=false) var sjdbFileChrStartEnd: File = _
  @Argument(doc="sjdbOverhang", required=false) var sjdbOverhang: Int = _
  @Input(doc="deps", required=false) var deps: List[File] = Nil
  
  @Output(doc="Output genome file", required=false) var outputGenome: File = _
  @Output(doc="Output SA file", required=false) var outputSA: File = _
  @Output(doc="Output SAindex file", required=false) var outputSAindex: File = _
  
  jobResourceRequests :+= "h_vmem=" + config.getAsString("vmem", "6G")
  nCoresRequest = Option(config.getThreads(8))
  
  def init() {
    this.addJobReportBinding("version", "NA")
    if (runmode == null) {
      outputSam = new File(outputDir + "/Aligned.out.sam")
      outputTab = new File(outputDir + "/SJ.out.tab")
    } else if (runmode == "genomeGenerate") {
      genomeDir = outputDir
      outputGenome = new File(genomeDir + "/Genome")
      outputSA = new File(genomeDir + "/SA")
      outputSAindex = new File(genomeDir + "/SAindex")
    }
  }
  
  def commandLine : String= {
    init()
    var cmd: String = required("cd",outputDir) + "&&" + required(star_exe)
    if (runmode != null && runmode == "genomeGenerate") { // Create index
      cmd += required("--runMode", runmode) +
        //required("--genomeDir", genomeDir) +
        required("--genomeFastaFiles", referenceFile)
    } else { // Aligner
      cmd += required("--readFilesIn", R1) + optional(R2)
    }
    cmd += required("--genomeDir", genomeDir) +
      optional("--sjdbFileChrStartEnd", sjdbFileChrStartEnd) +
      optional("--runThreadN", nCoresRequest)
    if (sjdbOverhang > 0) cmd += optional("--sjdbOverhang", sjdbOverhang)
    
    return cmd
  }
}