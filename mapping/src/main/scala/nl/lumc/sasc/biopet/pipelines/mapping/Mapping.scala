package nl.lumc.sasc.biopet.pipelines.mapping

import nl.lumc.sasc.biopet.function._
import nl.lumc.sasc.biopet.function.aligners._
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

class Mapping(private var globalConfig: Config) extends QScript with BiopetQScript {
  qscript =>
  @Argument(doc="Config Json file",shortName="config", required=false) var configfiles: List[File] = Nil
  @Input(doc="R1 fastq file", shortName="R1",required=true) var input_R1: File = _
  @Input(doc="R2 fastq file", shortName="R2", required=false) var input_R2: File = _
  @Argument(doc="Output directory", shortName="outputDir", required=true) var outputDir: String = _
  @Argument(doc="Output name", shortName="outputName", required=false) var outputName: String = _
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
  
  var referenceFile: File = _
  var paired: Boolean = false
  
  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    config = Config.mergeConfigs(globalConfig.getAsConfig("mapping"), globalConfig)
    if (aligner == null) aligner = config.getAsString("aligner", "bwa")
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
    
    if (outputName == null) outputName = RGID
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
    var bamFile:File = null
    if (aligner == "bwa") {
      val bwaCommand = new Bwa(config)
      bwaCommand.R1 = fastq_R1
      if (paired) bwaCommand.R2 = fastq_R2
      bwaCommand.RG = getReadGroup
      bwaCommand.output = new File(outputDir + outputName + ".sam")
      add(bwaCommand)
      bamFile = addSortSam(List(bwaCommand.output), swapExt(outputDir,bwaCommand.output,".sam",".bam"), outputDir)
    } else if (aligner == "star") {
      val starCommand = Star(config, fastq_R1, if (paired) fastq_R2 else null, outputDir)
      add(starCommand)
      bamFile = addAddOrReplaceReadGroups(List(starCommand.outputSam), new File(outputDir + outputName + ".bam"), outputDir)
    } else if (aligner == "star-2pass") {
      val starCommand_pass1 = Star(config, fastq_R1, if (paired) fastq_R2 else null, outputDir + "star-2pass/aln-pass1/")
      starCommand_pass1.afterGraph
      add(starCommand_pass1)
      
      val starCommand_reindex = new Star(config)
      starCommand_reindex.sjdbFileChrStartEnd = starCommand_pass1.outputTab
      starCommand_reindex.outputDir = qscript.outputDir + "star-2pass/re-index/" 
      starCommand_reindex.runmode = "genomeGenerate"
      starCommand_reindex.sjdbOverhang = 75
      starCommand_reindex.afterGraph
      add(starCommand_reindex)
      
      val starCommand_pass2 = Star(config, fastq_R1, if (paired) fastq_R2 else null, outputDir + "star-2pass/aln-pass2/")
      starCommand_pass2.genomeDir = starCommand_reindex.outputDir
      starCommand_pass2.afterGraph
      add(starCommand_pass2)
      bamFile = addAddOrReplaceReadGroups(List(starCommand_pass2.outputSam), new File(outputDir + outputName + ".bam"), outputDir)
    } else throw new IllegalStateException("Option Alginer: '" + aligner + "' is not valid")
    
    if (!skipMarkduplicates) bamFile = addMarkDuplicates(List(bamFile), swapExt(outputDir,bamFile,".bam",".dedup.bam"), outputDir)
    outputFiles += ("finalBamFile" -> bamFile)
  }
  
  def addSortSam(inputSam:List[File], outputFile:File, dir:String) : File = {
    val sortSam = new SortSam {
      this.input = inputSam
      this.createIndex = true
      this.output = outputFile
      this.memoryLimit = 2
      this.nCoresRequest = 2
      this.jobResourceRequests :+= "h_vmem=4G"
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
      
      this.RGID = qscript.RGID
      this.RGLB = qscript.RGLB
      this.RGPL = qscript.RGPL
      this.RGPU = qscript.RGPU
      this.RGSM = qscript.RGSM
      if (RGCN != null) this.RGCN = qscript.RGCN
      if (RGDS != null) this.RGDS = qscript.RGDS
    }
    add(addOrReplaceReadGroups)
    
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
    if (RGCN != null) RG += "CN:" + RGCN + "\\t"
    if (RGDS != null) RG += "DS" + RGDS + "\\t"
    if (RGDT != null) RG += "DT" + RGDT + "\\t"
    if (RGPI > 0) RG += "PI" + RGPI + "\\t"
    
    return RG.substring(0, RG.lastIndexOf("\\t"))
  }
  
  def loadRunConfig(runConfig:Config, sampleConfig:Config, runDir: String) {
    config = Config.mergeConfigs(globalConfig.getAsConfig("mapping"), globalConfig)
    val inputType = runConfig.getAsString("inputtype", config.getAsString("inputtype", "dna"))
    if (inputType == "rna") aligner = config.getAsString("rna_aligner", "star-2pass")
    input_R1 = runConfig.getAsString("R1", null)
    input_R2 = runConfig.getAsString("R2", null)
    paired = (input_R2 != null)
    RGLB = runConfig.getAsString("ID")
    RGSM = sampleConfig.get("ID").toString
    if (runConfig.contains("PL")) RGPL = runConfig.getAsString("PL")
    if (runConfig.contains("PU")) RGPU = runConfig.getAsString("PU")
    if (runConfig.contains("CN")) RGCN = runConfig.getAsString("CN")
    outputDir = runDir
  }
}

object Mapping extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/mapping/"
}
