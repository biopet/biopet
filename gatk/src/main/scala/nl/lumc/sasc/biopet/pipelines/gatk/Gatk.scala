package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.function._
import nl.lumc.sasc.biopet.function.aligners._
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.pipelines.mapping._
import nl.lumc.sasc.biopet.pipelines.flexiprep._
import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.extensions.picard._
import org.broadinstitute.sting.queue.function._
import scala.util.parsing.json._
import org.broadinstitute.sting.utils.variant._

class Gatk(val root:Configurable) extends QScript with MultiSampleQScript {
  qscript =>
  def this() = this(null)
  
  @Argument(doc="Only Sample",shortName="sample", required=false)
  val onlySample: String = ""
    
  var referenceFile: File = _
  var dbsnp: File = _
  var gvcfFiles: List[File] = Nil
  var finalBamFiles: List[File] = Nil
  
  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    referenceFile = config("referenceFile")
    if (configContains("dbsnp")) dbsnp = config("dbsnp")
    for (file <- config("gvcfFiles", Nil).getList) gvcfFiles :+= file.toString
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on gatk module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }
  
  def biopetScript() {
    if (onlySample.isEmpty) {
      runSamplesJobs
      
      //SampleWide jobs
      if (gvcfFiles.size > 0) {
        var vcfFile = addGenotypeGVCFs(gvcfFiles, outputDir + "recalibration/")
        if (config("inputtype", "dna").getString != "rna") {
          vcfFile = addVariantAnnotator(vcfFile, finalBamFiles, outputDir + "recalibration/")
          vcfFile = addSnpVariantRecalibrator(vcfFile, outputDir + "recalibration/")
          vcfFile = addIndelVariantRecalibrator(vcfFile, outputDir + "recalibration/")
        }
      } else logger.warn("No gVCFs to genotype")
    } else runSingleSampleJobs(onlySample)    
  }
  
  // Called for each sample
  def runSingleSampleJobs(sampleConfig:Map[String,Any]) : Map[String,List[File]] = {
    var outputFiles:Map[String,List[File]] = Map()
    var runBamfiles: List[File] = List()
    var sampleID: String = sampleConfig("ID").toString
    for ((run, runFiles) <- runRunsJobs(sampleConfig)) {
      runBamfiles +:= runFiles("FinalBam")
    }
    outputFiles += ("FinalBams" -> runBamfiles)
    
    if (runBamfiles.size > 0) {
      finalBamFiles ++= runBamfiles
      val gvcfFile = new File(outputDir + sampleID + "/" + sampleID + ".gvcf.vcf")
      addHaplotypeCaller(runBamfiles, gvcfFile)
      outputFiles += ("gvcf" -> List(gvcfFile))
      gvcfFiles :+= gvcfFile
    } else logger.warn("No bamfiles for variant calling for sample: " + sampleID)
    return outputFiles
  }
  
  // Called for each run from a sample
  def runSingleRunJobs(runConfig:Map[String,Any], sampleConfig:Map[String,Any]) : Map[String,File] = {
    var outputFiles:Map[String,File] = Map()
    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val runDir: String = outputDir + sampleID + "/run_" + runID + "/"
    var inputType = ""
    if (runConfig.contains("inputtype")) inputType = runConfig("inputtype").toString
    else inputType = config("inputtype", "dna").toString
    if (runConfig.contains("R1")) {
      val mapping = new Mapping(this)
      mapping.loadRunConfig(runConfig, sampleConfig, runDir)
      mapping.init
      mapping.biopetScript
      addAll(mapping.functions) // Add functions of mapping to curent function pool
      
      var bamFile:File = mapping.outputFiles("finalBamFile")
      if (inputType == "rna") bamFile = addSplitNCigarReads(bamFile,runDir)
      bamFile = addIndelRealign(bamFile,runDir) // Indel realigner
      bamFile = addBaseRecalibrator(bamFile,runDir) // Base recalibrator
      
      outputFiles += ("FinalBam" -> bamFile)
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for run: " + runConfig)    
    return outputFiles
  }
  
  def addMarkDuplicates(inputBams:List[File], outputFile:File, dir:String) : File = {
    val markDuplicates = new MarkDuplicates {
      this.input = inputBams
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
  
  def addIndelRealign(inputBam:File, dir:String): File = {
    val realignerTargetCreator = new RealignerTargetCreator with gatkArguments {
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".realign.intervals")
      this.jobResourceRequests :+= "h_vmem=5G"
      if (configContains("scattercount", "realignertargetcreator")) this.scatterCount = config("scattercount", 1, "realignertargetcreator")
    }
    add(realignerTargetCreator)

    val indelRealigner = new IndelRealigner with gatkArguments {
      this.I :+= inputBam
      this.targetIntervals = realignerTargetCreator.o
      this.o = swapExt(dir,inputBam,".bam",".realign.bam")
      if (configContains("scattercount", "indelrealigner")) this.scatterCount = config("scattercount", 1, "indelrealigner")
    }
    add(indelRealigner)
    
    return indelRealigner.o
  }
  
  def addBaseRecalibrator(inputBam:File, dir:String): File = {
    val baseRecalibrator = new BaseRecalibrator with gatkArguments {
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".baserecal")
      if (dbsnp != null) this.knownSites :+= dbsnp
      if (configContains("scattercount", "baserecalibrator")) this.scatterCount = config("scattercount", 1, "baserecalibrator")
      this.nct = 2
    }
    add(baseRecalibrator)

    val printReads = new PrintReads with gatkArguments {
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".baserecal.bam")
      this.BQSR = baseRecalibrator.o
      if (configContains("scattercount", "printreads")) this.scatterCount = config("scattercount", 1, "printreads")
    }
    add(printReads)
    
    return printReads.o
  }
  
  def addSplitNCigarReads(inputBam:File, dir:String) : File = {
    val splitNCigarReads = new SplitNCigarReads with gatkArguments {
      if (configContains("scattercount", "splitncigarreads")) this.scatterCount = config("scattercount", 1, "splitncigarreads")
      this.input_file = Seq(inputBam)
      this.out = swapExt(dir,inputBam,".bam",".split.bam")
      this.read_filter :+= "ReassignMappingQuality"
      
      this.U = org.broadinstitute.sting.gatk.arguments.ValidationExclusion.TYPE.ALLOW_N_CIGAR_READS
    }
    add(splitNCigarReads)
    
    return splitNCigarReads.out
  }
  
  def addHaplotypeCaller(bamfiles:List[File], outputfile:File): File = {
    val haplotypeCaller = new HaplotypeCaller with gatkArguments {
      if (configContains("scattercount", "haplotypecaller")) this.scatterCount = config("scattercount", 1, "haplotypecaller")
      this.input_file = bamfiles
      this.out = outputfile
      if (dbsnp != null) this.dbsnp = qscript.dbsnp
      this.nct = 3
      this.memoryLimit = this.nct * 2
      
      // GVCF options
      this.emitRefConfidence = org.broadinstitute.sting.gatk.walkers.haplotypecaller.HaplotypeCaller.ReferenceConfidenceMode.GVCF
      this.variant_index_type = GATKVCFIndexType.LINEAR
      this.variant_index_parameter = 128000
      
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
        this.stand_emit_conf = config("stand_emit_conf", 10, "haplotypecaller")
      }
    }
    add(haplotypeCaller)
    
    return haplotypeCaller.out
  }
  
  def addSnpVariantRecalibrator(inputVcf:File, dir:String): File = {
    val snpVariantRecalibrator = getVariantRecalibrator("snp")
    snpVariantRecalibrator.input +:= inputVcf
    snpVariantRecalibrator.recal_file = swapExt(dir, inputVcf,".vcf",".snp.recal")
    snpVariantRecalibrator.tranches_file = swapExt(dir, inputVcf,".vcf",".snp.tranches")
    add(snpVariantRecalibrator)

    val snpApplyRecalibration = getApplyRecalibration("snp")
    snpApplyRecalibration.input +:= inputVcf
    snpApplyRecalibration.recal_file = snpVariantRecalibrator.recal_file
    snpApplyRecalibration.tranches_file = snpVariantRecalibrator.tranches_file
    snpApplyRecalibration.out = swapExt(dir, inputVcf,".vcf",".snp.recal.vcf")
    add(snpApplyRecalibration)
    
    return snpApplyRecalibration.out
  }
  
  def addIndelVariantRecalibrator(inputVcf:File, dir:String): File = {
    val indelVariantRecalibrator = getVariantRecalibrator("indel")
    indelVariantRecalibrator.input +:= inputVcf
    indelVariantRecalibrator.recal_file = swapExt(dir, inputVcf,".vcf",".indel.recal")
    indelVariantRecalibrator.tranches_file = swapExt(dir, inputVcf,".vcf",".indel.tranches")
    add(indelVariantRecalibrator)

    val indelApplyRecalibration = getApplyRecalibration("indel")
    indelApplyRecalibration.input +:= inputVcf
    indelApplyRecalibration.recal_file = indelVariantRecalibrator.recal_file
    indelApplyRecalibration.tranches_file = indelVariantRecalibrator.tranches_file
    indelApplyRecalibration.out = swapExt(dir, inputVcf,".vcf",".indel.recal.vcf")
    add(indelApplyRecalibration)
    
    return indelApplyRecalibration.out
  }
  
  def getVariantRecalibrator(mode_arg:String) : VariantRecalibrator = {
    val variantRecalibrator = new VariantRecalibrator() with gatkArguments {
      if (mode_arg == "indel") {
        this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
        if (configContains("mills", "variantrecalibrator")) this.resource :+= new TaggedFile(config("mills", "", "variantrecalibrator").getString, "known=false,training=true,truth=true,prior=12.0")
      } else { // SNP
        this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
        if (configContains("hapmap", "variantrecalibrator")) this.resource +:= new TaggedFile(config("hapmap", "", "variantrecalibrator").getString, "known=false,training=true,truth=true,prior=15.0")
        if (configContains("omni", "variantrecalibrator")) this.resource +:= new TaggedFile(config("omni", "", "variantrecalibrator").getString, "known=false,training=true,truth=true,prior=12.0")
        if (configContains("1000G", "variantrecalibrator")) this.resource +:= new TaggedFile(config("1000G", "", "variantrecalibrator").getString, "known=false,training=true,truth=false,prior=10.0")
      }
      if (configContains("dbsnp", "variantrecalibrator")) this.resource :+= new TaggedFile(config("dbsnp", "", "variantrecalibrator").getString, "known=true,training=false,truth=false,prior=2.0")
      this.nt = 4
      this.memoryLimit = nt * 2
      this.an = Seq("QD","DP","FS","ReadPosRankSum","MQRankSum")
      if (configContains("minnumbadvariants", "variantrecalibrator")) this.minNumBadVariants = config("minnumbadvariants", "", "variantrecalibrator")
      if (configContains("maxgaussians", "variantrecalibrator")) this.maxGaussians = config("maxgaussians", "", "variantrecalibrator")
    }
    return variantRecalibrator
  }
  
  def getApplyRecalibration(mode_arg:String) : ApplyRecalibration = {
    val applyRecalibration = new ApplyRecalibration() with gatkArguments {
      if (mode_arg == "indel") {
        this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
        this.ts_filter_level = config("ts_filter_level", 99.0, "applyrecalibration")
      } else { // SNP
        this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
        this.ts_filter_level = config("ts_filter_level", 99.5, "applyrecalibration")
      }
      this.nt = 3
      this.memoryLimit = nt * 2
      if (configContains("scattercount", "applyrecalibration")) this.scatterCount = config("scattercount", 1, "applyrecalibration")
    }
    return applyRecalibration
  }
  
  def addGenotypeGVCFs(gvcfFiles: List[File], dir:String): File = {
    val genotypeGVCFs = new GenotypeGVCFs() with gatkArguments {
      this.variant = gvcfFiles
      this.annotation ++= Seq("FisherStrand", "QualByDepth", "ChromosomeCounts")
      if (configContains("scattercount", "genotypegvcfs")) this.scatterCount = config("scattercount", 1, "genotypegvcfs")
      this.out = new File(outputDir,"genotype.vcf")
    }
    add(genotypeGVCFs)
    return genotypeGVCFs.out
  }
  
  def addVariantAnnotator(inputvcf:File, bamfiles:List[File], dir:String): File = {
    val variantAnnotator = new VariantAnnotator with gatkArguments {
      this.variant = inputvcf
      this.input_file = bamfiles
      this.dbsnp = config("dbsnp", "variantannotator")
      this.out = swapExt(dir, inputvcf,".vcf",".anotated.vcf")
      if (configContains("scattercount", "variantannotator")) this.scatterCount = config("scattercount", 1, "variantannotator")
    }
    add(variantAnnotator)
    
    return variantAnnotator.out
  }
  
  trait gatkArguments extends CommandLineGATK {
    this.reference_sequence = referenceFile
    this.memoryLimit = 2
    this.jobResourceRequests :+= "h_vmem=4G"
  }
}

object Gatk extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/Gatk.class"
}
