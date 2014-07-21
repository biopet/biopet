package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.function._
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.{BaseRecalibrator, CommandLineGATK, HaplotypeCaller, IndelRealigner, PrintReads, RealignerTargetCreator, GenotypeGVCFs, AnalyzeCovariates}
import org.broadinstitute.gatk.queue.function._
import org.broadinstitute.gatk.utils.commandline.{Input, Output, Argument}
import org.broadinstitute.gatk.utils.variant.GATKVCFIndexType

class GatkVariantcalling(val root:Configurable) extends QScript with BiopetQScript {
  def this() = this(null)
  
  @Input(doc="Bam files (should be deduped bams)", shortName="BAM")
  var inputBams: List[File] = Nil
  
  @Argument(doc="Reference", shortName="R", required=false)
  var reference: File = _
  
  @Argument(doc="Dbsnp", shortName="dbsnp", required=false)
  var dbsnp: File = _
  
  @Argument(doc="OutputName", required=false)
  var outputName: String = "hc"
  
  @Output(doc="OutputFile", required=false)
  var outputFile: File = _
  
  var gvcfMode = true
  var singleGenotyping = false
  
  def init() {
    if (gvcfMode) gvcfMode = config("gvcfmode", true)
    if (!singleGenotyping) singleGenotyping = config("singlegenotyping", false)
    if (reference == null) reference = config("reference")
    if (dbsnp == null && configContains("dbsnp")) dbsnp = config("dbsnp")
    if (outputFile == null) outputFile = outputDir + outputName + (if (gvcfMode) ".gvcf.vcf" else ".vcf")
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on gatk module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }
  
  def biopetScript() {
    var bamFiles: List[File] = Nil
    for (inputBam <- inputBams) {
      var bamFile = if (dbsnp != null) addIndelRealign(inputBam, outputDir) else inputBam
      bamFiles :+= addBaseRecalibrator(bamFile, outputDir)
    }
    addHaplotypeCaller(bamFiles, outputFile)
    if (gvcfMode && singleGenotyping) addGenotypeGVCFs(List(outputFile), outputDir)
  }
  
  trait gatkArguments extends CommandLineGATK {
    this.reference_sequence = reference
    this.memoryLimit = 2
    this.jobResourceRequests :+= "h_vmem=4G"
  }
  
  def addIndelRealign(inputBam:File, dir:String): File = {
    val realignerTargetCreator = new RealignerTargetCreator with gatkArguments {
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".realign.intervals")
      this.jobResourceRequests :+= "h_vmem=5G"
      if (configContains("scattercount", "realignertargetcreator")) this.scatterCount = config("scattercount", 1, "realignertargetcreator")
    }
    realignerTargetCreator.isIntermediate = true
    add(realignerTargetCreator)
    
    val indelRealigner = new IndelRealigner with gatkArguments {
      this.I :+= inputBam
      this.targetIntervals = realignerTargetCreator.o
      this.o = swapExt(dir,inputBam,".bam",".realign.bam")
      if (configContains("scattercount", "indelrealigner")) this.scatterCount = config("scattercount", 1, "indelrealigner")
    }
    indelRealigner.isIntermediate = true
    add(indelRealigner)
    
    return indelRealigner.o
  }
  
  def addBaseRecalibrator(inputBam:File, dir:String): File = {
    val baseRecalibrator = new BaseRecalibrator with gatkArguments {
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".baserecal")
      if (dbsnp != null) this.knownSites :+= dbsnp
      if (configContains("scattercount", "baserecalibrator")) this.scatterCount = config("scattercount", 1, "baserecalibrator")
      this.nct = config("threads", 1, "baserecalibrator")
    }
    add(baseRecalibrator)
    
    val baseRecalibratorAfter = new BaseRecalibrator with gatkArguments {
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".baserecal.after")
      this.BQSR = baseRecalibrator.o
      if (dbsnp != null) this.knownSites :+= dbsnp
      if (configContains("scattercount", "baserecalibrator")) this.scatterCount = config("scattercount", 1, "baserecalibrator")
      this.nct = config("threads", 1, "baserecalibrator")
    }
    add(baseRecalibratorAfter)
    
    val analyzeCovariates = new AnalyzeCovariates with gatkArguments {
      this.before = baseRecalibrator.o
      this.after = baseRecalibratorAfter.o
      this.plots = swapExt(dir,inputBam,".bam",".baserecal.pdf")
    }
    add(analyzeCovariates)
    
    val printReads = new PrintReads with gatkArguments {
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".baserecal.bam")
      this.BQSR = baseRecalibrator.o
      if (configContains("scattercount", "printreads")) this.scatterCount = config("scattercount", 1, "printreads")
    }
    printReads.isIntermediate = true
    add(printReads)
    
    return printReads.o
  }
  
  def addHaplotypeCaller(bamfiles:List[File], outputfile:File): File = {
    val haplotypeCaller = new HaplotypeCaller with gatkArguments {
      this.min_mapping_quality_score = config("minMappingQualityScore", 20, "haplotypecaller")
      if (configContains("scattercount", "haplotypecaller")) this.scatterCount = config("scattercount", 1, "haplotypecaller")
      this.input_file = bamfiles
      this.out = outputfile
      if (configContains("dbsnp")) this.dbsnp = config("dbsnp")
      this.nct = config("threads", 3, "haplotypecaller")
      this.memoryLimit = this.nct * 2
      
      // GVCF options
      if (gvcfMode) {
        this.emitRefConfidence = org.broadinstitute.gatk.tools.walkers.haplotypecaller.ReferenceConfidenceMode.GVCF
        this.variant_index_type = GATKVCFIndexType.LINEAR
        this.variant_index_parameter = 128000
      }
      
      val inputType:String = config("inputtype", "dna")
      if (inputType == "rna") {
        this.dontUseSoftClippedBases = config("dontusesoftclippedbases", true, "haplotypecaller")
        this.recoverDanglingHeads = config("recoverdanglingheads", true, "haplotypecaller")
        this.stand_call_conf = config("stand_call_conf", 20, "haplotypecaller")
        this.stand_emit_conf = config("stand_emit_conf", 20, "haplotypecaller")
      } else {
        this.dontUseSoftClippedBases = config("dontusesoftclippedbases", false, "haplotypecaller")
        this.recoverDanglingHeads = config("recoverdanglingheads", false, "haplotypecaller")
        this.stand_call_conf = config("stand_call_conf", 30, "haplotypecaller")
        this.stand_emit_conf = config("stand_emit_conf", 30, "haplotypecaller")
      }
    }
    add(haplotypeCaller)
    
    return haplotypeCaller.out
  }
  
  def addGenotypeGVCFs(gvcfFiles: List[File], dir:String): File = {
    val genotypeGVCFs = new GenotypeGVCFs() with gatkArguments {
      this.variant = gvcfFiles
      this.annotation ++= Seq("FisherStrand", "QualByDepth", "ChromosomeCounts")
      if (configContains("dbsnp")) this.dbsnp = config("dbsnp")
      if (configContains("scattercount", "genotypegvcfs")) this.scatterCount = config("scattercount", 1, "genotypegvcfs")
      this.out = outputDir + outputName + ".vcf"
      this.stand_call_conf = config("stand_call_conf", 30, "genotypegvcfs")
      this.stand_emit_conf = config("stand_emit_conf", 30, "genotypegvcfs")
    }
    add(genotypeGVCFs)
    return genotypeGVCFs.out
  }
}

object GatkVariantcalling extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/GatkVariantcalling.class"
}
