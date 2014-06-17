package nl.lumc.sasc.biopet.function.aligners

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.sys.process._

class Star(val globalConfig: Config) extends BiopetCommandLineFunction {
  @Input(doc="The reference file for the bam files.", required=false)
  var referenceFile: File = new File(config.getAsString("referenceFile"))
  
  @Input(doc="Fastq file R1", required=false)
  var R1: File = _
  
  @Input(doc="Fastq file R2", required=false)
  var R2: File = _
  
  @Output(doc="Output SAM file", required=false)
  var outputSam: File = _
  
  @Output(doc="Output tab file", required=false)
  var outputTab: File = _
  
  @Input(doc="sjdbFileChrStartEnd file", required=false)
  var sjdbFileChrStartEnd: File = _
  
  @Output(doc="Output genome file", required=false)
  var outputGenome: File = _
  
  @Output(doc="Output SA file", required=false)
  var outputSA: File = _
  
  @Output(doc="Output SAindex file", required=false)
  var outputSAindex: File = _
  
  executeble = config.getAsString("exe", "STAR")
  
  @Argument(doc="Output Directory")
  var outputDir: String = _
  
  var genomeDir: String = config.getAsString("genomeDir", referenceFile.getParent + "/star/")
  var runmode: String = _
  var sjdbOverhang: Int = _
  var outFileNamePrefix: String = _
  
  override val defaultVmem = "6G"
  override val defaultThreads = 8
  
  override def afterGraph() {
    if (outFileNamePrefix != null && !outFileNamePrefix.endsWith(".")) outFileNamePrefix +="."
    if (!outputDir.endsWith("/")) outputDir += "/"
    val prefix = if (outFileNamePrefix != null) outputDir+outFileNamePrefix else outputDir
    if (runmode == null) {
      outputSam = new File(prefix + "Aligned.out.sam")
      outputTab = new File(prefix + "SJ.out.tab")
    } else if (runmode == "genomeGenerate") {
      genomeDir = outputDir
      outputGenome = new File(prefix + "Genome")
      outputSA = new File(prefix + "SA")
      outputSAindex = new File(prefix + "SAindex")
    }
  }
  
  def cmdLine : String = {
    var cmd: String = required("cd",outputDir) + "&&" + required(executeble)
    if (runmode != null && runmode == "genomeGenerate") { // Create index
      cmd += required("--runMode", runmode) +
        required("--genomeFastaFiles", referenceFile)
    } else { // Aligner
      cmd += required("--readFilesIn", R1) + optional(R2)
    }
    cmd += required("--genomeDir", genomeDir) +
      optional("--sjdbFileChrStartEnd", sjdbFileChrStartEnd) +
      optional("--runThreadN", nCoresRequest) +
      optional("--outFileNamePrefix", outFileNamePrefix)
    if (sjdbOverhang > 0) cmd += optional("--sjdbOverhang", sjdbOverhang)
    
    return cmd
  }
}

object Star {
  def apply(config:Config, R1:File, R2:File, outputDir:String): Star = {
    val star = new Star(config)
    star.R1 = R1
    if (R2 != null) star.R2 = R2
    star.outputDir = outputDir
    return star
  }
}