package nl.lumc.sasc.biopet.pipelines.mapping

import nl.lumc.sasc.biopet.function._
import nl.lumc.sasc.biopet.function.aligners._
import java.util.Date
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.pipelines.flexiprep._
import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.extensions.picard.MarkDuplicates
import org.broadinstitute.sting.queue.extensions.picard.SortSam
import org.broadinstitute.sting.queue.extensions.picard.AddOrReplaceReadGroups
import org.broadinstitute.sting.queue.function._
import scala.util.parsing.json._
import org.broadinstitute.sting.utils.variant._

class Mapping(val root:Configurable) extends QScript with BiopetQScript {
  qscript =>
  def this() = this(null)
  
  @Input(doc="R1 fastq file", shortName="R1",required=true)
  var input_R1: File = _
  
  @Input(doc="R2 fastq file", shortName="R2", required=false)
  var input_R2: File = _
  
  @Argument(doc="Output name", shortName="outputName", required=false)
  var outputName: String = _
  
  @Argument(doc="Skip flexiprep", shortName="skipflexiprep", required=false)
  var skipFlexiprep: Boolean = false
  
  @Argument(doc="Skip mark duplicates", shortName="skipmarkduplicates", required=false)
  var skipMarkduplicates: Boolean = false
  
  @Argument(doc="Alginer", shortName="ALN", required=false)
  var aligner: String = _
  
  @Argument(doc="Reference", shortName="R", required=false)
  var referenceFile: File = _
  
  // Readgroup items
  @Argument(doc="Readgroup ID", shortName="RGID", required=false)
  var RGID: String = _
  
  @Argument(doc="Readgroup Library", shortName="RGLB", required=false)
  var RGLB: String = _
  
  @Argument(doc="Readgroup Platform", shortName="RGPL", required=false)
  var RGPL: String = _
  
  @Argument(doc="Readgroup platform unit", shortName="RGPU", required=false)
  var RGPU: String = _
  
  @Argument(doc="Readgroup sample", shortName="RGSM", required=false)
  var RGSM: String = _
  
  @Argument(doc="Readgroup sequencing center", shortName="RGCN", required=false)
  var RGCN: String = _
  
  @Argument(doc="Readgroup description", shortName="RGDS", required=false)
  var RGDS: String = _
  
  @Argument(doc="Readgroup sequencing date", shortName="RGDT", required=false)
  var RGDT: Date = _
  
  @Argument(doc="Readgroup predicted insert size", shortName="RGPI", required=false)
  var RGPI: Int = _
  
  
  
  var paired: Boolean = false
  
  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    var inputtype:String = config("inputtype", "dna")
    if (aligner == null) {
      if (inputtype == "rna") aligner = config("aligner", "star-2pass")
      else aligner = config("aligner", "bwa")
    }
    if (referenceFile == null) referenceFile = config("referenceFile")
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on mapping module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
    if (input_R1 == null) throw new IllegalStateException("Missing Fastq R1 on mapping module")
    paired = (input_R2 != null)
    
    if (RGLB == null && configContains("RGLB")) RGLB = config("RGLB")
    else if (RGLB == null) throw new IllegalStateException("Missing Readgroup library on mapping module")
    if (RGSM == null && configContains("RGSM")) RGSM = config("RGSM")
    else if (RGLB == null) throw new IllegalStateException("Missing Readgroup sample on mapping module")
    if (RGID == null && configContains("RGID")) RGID = config("RGID")
    else if (RGID == null && RGSM != null && RGLB != null) RGID = RGSM + "-" + RGLB
    else if (RGID == null) throw new IllegalStateException("Missing Readgroup ID on mapping module")    
    
    if (RGPL == null) RGPL = config("RGPL", "illumina")
    if (RGPU == null) RGPU = config("RGPU", "na")
    if (RGCN == null && configContains("RGCN")) RGCN = config("RGCN")
    if (RGDS == null && configContains("RGDS")) RGDS = config("RGDS")
    
    if (outputName == null) outputName = RGID
  }
  
  def biopetScript() {
    var fastq_R1: String = input_R1
    var fastq_R2: String = if (paired) input_R2 else ""
    if (!skipFlexiprep) {
      val flexiprep = new Flexiprep(this)
      flexiprep.input_R1 = fastq_R1
      if (paired) flexiprep.input_R2 = fastq_R2
      flexiprep.outputDir = outputDir + "flexiprep/"
      flexiprep.init
      flexiprep.biopetScript
      addAll(flexiprep.functions) // Add function of flexiprep to curent function pool
      fastq_R1 = flexiprep.outputFiles("output_R1")
      if (paired) fastq_R2 = flexiprep.outputFiles("output_R2")
    }
    var bamFile:File = null
    if (aligner == "bwa") {
      val bwaCommand = new Bwa(this)
      bwaCommand.R1 = fastq_R1
      if (paired) bwaCommand.R2 = fastq_R2
      bwaCommand.RG = getReadGroup
      bwaCommand.output = new File(outputDir + outputName + ".sam")
      add(bwaCommand, isIntermediate = true)
      bamFile = addSortSam(List(bwaCommand.output), swapExt(outputDir,bwaCommand.output,".sam",".bam"), outputDir)
    } else if (aligner == "star") {
      val starCommand = Star(this, fastq_R1, if (paired) fastq_R2 else null, outputDir, isIntermediate = true)
      add(starCommand)
      bamFile = addAddOrReplaceReadGroups(List(starCommand.outputSam), new File(outputDir + outputName + ".bam"), outputDir)
    } else if (aligner == "star-2pass") {
      val star2pass = Star._2pass(this, fastq_R1, if (paired) fastq_R2 else null, outputDir, isIntermediate = true)
      addAll(star2pass._2)
      bamFile = addAddOrReplaceReadGroups(List(star2pass._1), new File(outputDir + outputName + ".bam"), outputDir)
    } else throw new IllegalStateException("Option Alginer: '" + aligner + "' is not valid")
    
    if (!skipMarkduplicates) bamFile = addMarkDuplicates(List(bamFile), swapExt(outputDir,bamFile,".bam",".dedup.bam"), outputDir)
    outputFiles += ("finalBamFile" -> bamFile)
  }
  
  def addSortSam(inputSam:List[File], outputFile:File, dir:String) : File = {
    val sortSam = new SortSam
    sortSam.input = inputSam
    sortSam.createIndex = true
    sortSam.output = outputFile
    sortSam.memoryLimit = 2
    sortSam.nCoresRequest = 2
    sortSam.jobResourceRequests :+= "h_vmem=4G"
    if (!skipMarkduplicates) sortSam.isIntermediate = true
    add(sortSam)
    
    return sortSam.output
  }
  
  def addAddOrReplaceReadGroups(inputSam:List[File], outputFile:File, dir:String) : File = {
    val addOrReplaceReadGroups = new AddOrReplaceReadGroups
    addOrReplaceReadGroups.input = inputSam
    addOrReplaceReadGroups.output = outputFile
    addOrReplaceReadGroups.createIndex = true
    addOrReplaceReadGroups.memoryLimit = 2
    addOrReplaceReadGroups.nCoresRequest = 2
    addOrReplaceReadGroups.jobResourceRequests :+= "h_vmem=4G"

    addOrReplaceReadGroups.RGID = RGID
    addOrReplaceReadGroups.RGLB = RGLB
    addOrReplaceReadGroups.RGPL = RGPL
    addOrReplaceReadGroups.RGPU = RGPU
    addOrReplaceReadGroups.RGSM = RGSM
    if (RGCN != null) addOrReplaceReadGroups.RGCN = RGCN
    if (RGDS != null) addOrReplaceReadGroups.RGDS = RGDS
    if (!skipMarkduplicates) addOrReplaceReadGroups.isIntermediate = true
    add(addOrReplaceReadGroups)
    
    return addOrReplaceReadGroups.output
  }
  
  def addMarkDuplicates(input_Bams:List[File], outputFile:File, dir:String) : File = {
    val markDuplicates = new MarkDuplicates
    markDuplicates.input = input_Bams
    markDuplicates.output = outputFile
    markDuplicates.REMOVE_DUPLICATES = false
    markDuplicates.metrics = swapExt(dir,outputFile,".bam",".metrics")
    markDuplicates.outputIndex = swapExt(dir,markDuplicates.output,".bam",".bai")
    markDuplicates.memoryLimit = 2
    markDuplicates.jobResourceRequests :+= "h_vmem=4G"
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
  
  def loadRunConfig(runConfig:Map[String,Any], sampleConfig:Map[String,Any], runDir: String) {
    logger.debug("Mapping runconfig: " + runConfig)
    var inputType = ""
    if (runConfig.contains("inputtype")) inputType = runConfig("inputtype").toString
    else inputType = config("inputtype", "dna")
    if (inputType == "rna") aligner = config("rna_aligner", "star-2pass")
    if (runConfig.contains("R1")) input_R1 = runConfig("R1").toString
    if (runConfig.contains("R2")) input_R2 = runConfig("R2").toString
    paired = (input_R2 != null)
    RGLB = runConfig("ID").toString
    RGSM = sampleConfig("ID").toString
    if (runConfig.contains("PL")) RGPL = runConfig("PL").toString
    if (runConfig.contains("PU")) RGPU = runConfig("PU").toString
    if (runConfig.contains("CN")) RGCN = runConfig("CN").toString
    outputDir = runDir
  }
}

object Mapping extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/mapping/Mapping.class"
}
