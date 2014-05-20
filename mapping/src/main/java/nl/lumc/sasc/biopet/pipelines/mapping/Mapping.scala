package nl.lumc.sasc.biopet.pipelines.mapping

import nl.lumc.sasc.biopet.wrappers._
import java.util.Date
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.pipelines.flexiprep._
import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.extensions.picard.MarkDuplicates
import org.broadinstitute.sting.queue.extensions.picard.SortSam
import org.broadinstitute.sting.queue.extensions.picard.AddOrReplaceReadGroups
import org.broadinstitute.sting.queue.function._
import scala.util.parsing.json._
import org.broadinstitute.sting.utils.variant._

class Mapping(private var globalConfig: Config) extends QScript {
  @Argument(doc="Config Json file",shortName="config", required=false) var configfiles: List[File] = Nil
  @Input(doc="R1 fastq file", shortName="R1",required=true) var input_R1: File = _
  @Input(doc="R2 fastq file", shortName="R2", required=false) var input_R2: File = _
  @Argument(doc="Output directory", shortName="outputDir", required=true) var outputDir: String = _
  @Argument(doc="Output name", shortName="outputName", required=true) var outputName: String = _
  @Argument(doc="Skip flexiprep", shortName="skipflexiprep", required=false) var skipFlexiprep: Boolean = false
  @Argument(doc="Skip mark duplicates", shortName="skipmarkduplicates", required=false) var skipMarkduplicates: Boolean = false
  @Argument(doc="Alginer", shortName="ALN", required=false) var aligner: String = _
  
  // Readgroup items
  @Argument(doc="Readgroup ID", shortName="RGID", required=false) var RGID: String = _
  @Argument(doc="Readgroup Library", shortName="RGLB", required=false) var RGLB: String = _
  @Argument(doc="Readgroup Platform", shortName="RGPL", required=false) var RGPL: String = _
  @Argument(doc="Readgroup platform unit", shortName="RGPU", required=false) var RGPU: String = _
  @Argument(doc="Readgroup sample", shortName="RGSM", required=false) var RGSM: String = _
  @Argument(doc="Readgroup sequencing center", shortName="RGCN", required=false) var RGCN: String = _
  @Argument(doc="Readgroup description", shortName="RGDS", required=false) var RGDS: String = _
  @Argument(doc="Readgroup sequencing date", shortName="RGDT", required=false) var RGDT: Date = _
  @Argument(doc="Readgroup predicted insert size", shortName="RGPI", required=false) var RGPI: Int = _
  
  def this() = this(new Config())
  
  var config: Config = _
  var referenceFile: File = _
  var outputFiles:Map[String,File] = Map()
  var paired: Boolean = false
  
  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    config = Config.mergeConfigs(globalConfig.getAsConfig("mapping"), globalConfig)
    if (aligner == null) aligner = "bwa"
    referenceFile = config.getAsString("referenceFile")
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on mapping module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
    if (input_R1 == null) throw new IllegalStateException("Missing Fastq R1 on mapping module")
    paired = (input_R2 != null)
    
    if (RGLB == null && config.contains("RGLB")) RGLB = config.getAsString("RGLB")
    else if (RGLB == null) throw new IllegalStateException("Missing Readgroup library on mapping module")
    if (RGSM == null && config.contains("RGSM")) RGSM = config.getAsString("RGSM")
    else if (RGLB == null) throw new IllegalStateException("Missing Readgroup sample on mapping module")
    if (RGID == null && config.contains("RGID")) RGID = config.getAsString("RGID")
    else if (RGID == null && RGSM != null && RGLB != null) RGID = RGSM + "-" + RGLB
    else if (RGID == null) throw new IllegalStateException("Missing Readgroup ID on mapping module")    
    
    if (RGPL == null) RGPL = config.getAsString("RGPL", "illumina")
    if (RGPU == null) RGPU = config.getAsString("RGPU", "na")
    if (RGCN == null && config.contains("RGCN")) RGCN = config.getAsString("RGCN")
    if (RGDS == null && config.contains("RGDS")) RGDS = config.getAsString("RGDS")
  }
  
  def script() {
    this.init()
    var fastq_R1: String = input_R1
    var fastq_R2: String = if (paired) input_R2 else ""
    if (!skipFlexiprep) {
      val flexiprep = new Flexiprep(config)
      flexiprep.input_R1 = fastq_R1
      if (paired) flexiprep.input_R2 = fastq_R2
      flexiprep.outputDir = outputDir + "flexiprep/"
      flexiprep.script
      addAll(flexiprep.functions) // Add function of flexiprep to curent function pool
      fastq_R1 = flexiprep.outputFiles("output_R1")
      if (paired) fastq_R2 = flexiprep.outputFiles("output_R2")
    }
    var bamFile:File = ""
    if (aligner == "bwa") {
      val bwaCommand = new Bwa(config) { R1 = fastq_R1; if (paired) R2 = fastq_R2; 
                                        RG = getReadGroup; output = new File(outputDir + outputName + ".sam") }
      add(bwaCommand)
      bamFile = addSortSam(List(bwaCommand.output), swapExt(outputDir,bwaCommand.output,".sam",".bam"), outputDir)
    } else if (aligner == "star") {
      val starCommand = new Star(config) { R1 = fastq_R1; if (paired) R2 = fastq_R2; this.outputDir = outputDir }
      add(starCommand)
      bamFile = addAddOrReplaceReadGroups(List(starCommand.outputSam), swapExt(outputDir,starCommand.outputSam,".sam",".bam"), outputDir)
    }
    
    if (!skipMarkduplicates) bamFile = addMarkDuplicates(List(bamFile), swapExt(outputDir,bamFile,".bam",".dedup.bam"), outputDir)
  }
    
  def addSortSam(inputSam:List[File], outputFile:File, dir:String) : File = {
    val sortSam = new SortSam {
      input = inputSam
      createIndex = true
      output = outputFile
      memoryLimit = 2
      nCoresRequest = 2
      jobResourceRequests :+= "h_vmem=4G"
    }
    add(sortSam)
    
    return sortSam.output
  }
  
  def addAddOrReplaceReadGroups(inputSam:List[File], outputFile:File, dir:String) : File = {
    val addOrReplaceReadGroups = new AddOrReplaceReadGroups {
      this.input = inputSam
      this.output = outputFile
      this.createIndex = true
      this.memoryLimit = 2
      this.nCoresRequest = 2
      this.jobResourceRequests :+= "h_vmem=4G"

      this.RGLB = RGLB
      this.RGPL = RGPL
      this.RGPU = RGPU
      this.RGSM = RGSM
      if (RGCN != null) this.RGCN = RGCN
      if (RGDS != null) this.RGDS = RGDS
    }
    return addOrReplaceReadGroups.output
  }
  
  def addMarkDuplicates(input_Bams:List[File], outputFile:File, dir:String) : File = {
    val markDuplicates = new MarkDuplicates {
      this.input = input_Bams
      this.output = outputFile
      this.REMOVE_DUPLICATES = false
      this.metrics = swapExt(dir,outputFile,".bam",".metrics")
      this.outputIndex = swapExt(dir,this.output,".bam",".bai")
      this.memoryLimit = 2
      this.jobResourceRequests :+= "h_vmem=4G"
    }
    add(markDuplicates)
    
    return markDuplicates.output
  }
  
  def getReadGroup() : String = {
    var RG: String = "@RG\\t" + "ID:" + RGID + "\\t"
    RG += "LB:" + RGLB + "\\t"
    RG += "PL:" + RGPL + "\\t"
    RG += "PU:" + RGPU + "\\t"
    RG += "SM:" + RGSM + "\\t"
    RG += "CN:" + RGCN + "\\t"
    RG += "DS" + RGDS + "\\t"
    RG += "DT" + RGDT + "\\t"
    RG += "PI" + RGPI + "\\t"
    
    return RG.substring(0, RG.lastIndexOf("\\t"))
  }
}
