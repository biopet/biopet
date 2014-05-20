package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.wrappers._
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.pipelines.flexiprep._
import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.extensions.picard._
import org.broadinstitute.sting.queue.function._
import scala.util.parsing.json._
import org.broadinstitute.sting.utils.variant._

class Gatk(private var globalConfig: Config) extends QScript {
  @Argument(doc="Config Json file",shortName="config") var configfiles: List[File] = Nil
  @Argument(doc="Only Sample",shortName="sample", required=false) var onlySample: String = _
  @Argument(doc="Output directory", shortName="outputDir", required=true) var outputDir: String = _
  def this() = this(new Config())
  var config: Config = _
  var scatterCount: Int = _
  var referenceFile: File = _
  var dbsnp: File = _
  var gvcfFiles: List[File] = Nil
  
  trait gatkArguments extends CommandLineGATK {
    this.reference_sequence = referenceFile
    this.memoryLimit = 2
    this.jobResourceRequests :+= "h_vmem=4G"
  }
  
  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    config = Config.mergeConfigs(globalConfig.getAsConfig("gatk"), globalConfig)
    referenceFile = config.getAsString("referenceFile")
    dbsnp = config.getAsString("dbsnp")
    gvcfFiles = config.getAsListOfStrings("gvcfFiles", Nil)
    scatterCount = config.getAsInt("scatterCount", 1)
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on flexiprep module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }
  
  def script() {
    this.init()
    if (config.contains("Samples")) for ((key,value) <- config.getAsMap("Samples")) {
      if (onlySample == null || onlySample == key) {
        var sample:Config = config.getAsConfig("Samples").getAsConfig(key)
        if (sample.getAsString("ID") == key) { 
          var files:Map[String,List[File]] = sampleJobs(sample)
          if (files.contains("gvcf")) for (file <- files("gvcf")) gvcfFiles :+= file
        } else logger.warn("Key is not the same as ID on value for sample")
      } else logger.info("Skipping Sample: " + key)
    } else logger.warn("No Samples found in config")
    
    if (onlySample == null) {
      //SampleWide jobs
      if (gvcfFiles.size > 0) {
        val genotypeGVCFs = new GenotypeGVCFs() with gatkArguments
        genotypeGVCFs.variant = gvcfFiles
        genotypeGVCFs.scatterCount = scatterCount
        genotypeGVCFs.out = new File(outputDir,"final.vcf")
        add(genotypeGVCFs)
        
        //Snp recal
        val snpVariantRecalibrator = new VariantRecalibrator() with gatkArguments
        snpVariantRecalibrator.input +:= genotypeGVCFs.out
        snpVariantRecalibrator.nt = 8
        snpVariantRecalibrator.recal_file = swapExt(genotypeGVCFs.out,".vcf",".snp.recal")
        snpVariantRecalibrator.tranches_file = swapExt(genotypeGVCFs.out,".vcf",".snp.tranches")
        snpVariantRecalibrator.resource :+= new TaggedFile(config.getAsString("hapmap"), "known=false,training=true,truth=true,prior=15.0")
        snpVariantRecalibrator.resource :+= new TaggedFile(config.getAsString("omni"), "known=false,training=true,truth=true,prior=12.0")
        snpVariantRecalibrator.resource :+= new TaggedFile(config.getAsString("1000G"), "known=false,training=true,truth=false,prior=10.0")
        snpVariantRecalibrator.resource :+= new TaggedFile(config.getAsString("dbsnp"), "known=true,training=false,truth=false,prior=2.0")
        snpVariantRecalibrator.an = Seq("QD","MQ","MQRankSum","ReadPosRankSum","FS","DP","InbreedingCoeff")
        snpVariantRecalibrator.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
        add(snpVariantRecalibrator)
        
        val snpApplyRecalibration = new ApplyRecalibration() with gatkArguments
        snpApplyRecalibration.input +:= genotypeGVCFs.out
        snpApplyRecalibration.recal_file = snpVariantRecalibrator.recal_file
        snpApplyRecalibration.tranches_file = snpVariantRecalibrator.tranches_file
        snpApplyRecalibration.out = swapExt(genotypeGVCFs.out,".vcf",".snp.recal.vcf")
        snpApplyRecalibration.ts_filter_level = 99.5
        snpApplyRecalibration.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
        snpApplyRecalibration.nt = 3
        snpApplyRecalibration.scatterCount = scatterCount
        add(snpApplyRecalibration)
        
        //indel recal
        val indelVariantRecalibrator = new VariantRecalibrator() with gatkArguments
        indelVariantRecalibrator.input +:= genotypeGVCFs.out
        indelVariantRecalibrator.nt = 8
        indelVariantRecalibrator.recal_file = swapExt(genotypeGVCFs.out,".vcf",".indel.recal")
        indelVariantRecalibrator.tranches_file = swapExt(genotypeGVCFs.out,".vcf",".indel.tranches")
        indelVariantRecalibrator.resource :+= new TaggedFile(config.getAsString("mills"), "known=false,training=true,truth=true,prior=12.0")
        indelVariantRecalibrator.resource :+= new TaggedFile(config.getAsString("dbsnp"), "known=true,training=false,truth=false,prior=2.0")
        indelVariantRecalibrator.an = Seq("QD","DP","FS","ReadPosRankSum","MQRankSum","InbreedingCoeff")
        indelVariantRecalibrator.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
        add(indelVariantRecalibrator)
        
        val indelApplyRecalibration = new ApplyRecalibration() with gatkArguments
        indelApplyRecalibration.input +:= genotypeGVCFs.out
        indelApplyRecalibration.recal_file = indelVariantRecalibrator.recal_file
        indelApplyRecalibration.tranches_file = indelVariantRecalibrator.tranches_file
        indelApplyRecalibration.out = swapExt(genotypeGVCFs.out,".vcf",".indel.recal.vcf")
        indelApplyRecalibration.ts_filter_level = 99.0
        indelApplyRecalibration.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
        indelApplyRecalibration.nt = 3
        indelApplyRecalibration.scatterCount = scatterCount
        add(indelApplyRecalibration)
        
        // merge snp and indels
        val catVariants = new CatVariants()
        catVariants.variant = Seq(snpApplyRecalibration.out,indelApplyRecalibration.out)
        catVariants.outputFile = swapExt(genotypeGVCFs.out,".vcf",".recal.vcf")
        add(catVariants)
      } else logger.warn("No gVCFs to genotype")
      
      
    }
  }
  
  // Called for each sample
  def sampleJobs(sampleConfig:Config) : Map[String,List[File]] = {
    var outputFiles:Map[String,List[File]] = Map()
    outputFiles += ("FinalBams" -> List())
    var runs:List[Map[String,File]] = Nil
    if (sampleConfig.contains("ID")) {
      var sampleID: String = sampleConfig.getAsString("ID")
      this.logger.info("Starting generate jobs for sample: " + sampleID)
      for (key <- sampleConfig.getAsMap("Runs").keySet) {
        var runConfig = sampleConfig.getAsConfig("Runs").getAsConfig(key)
        var run: Map[String,File] = runJobs(runConfig, sampleConfig)
        var FinalBams:List[File] = outputFiles("FinalBams") 
        if (run.contains("FinalBam")) FinalBams :+= run("FinalBam")
        else logger.warn("No Final bam for Sample: " + sampleID + "  Run: " + runConfig)
        outputFiles += ("FinalBams" -> FinalBams)
        runs +:= run
      }
      
      // Variant calling
      val haplotypeCaller = new HaplotypeCaller with gatkArguments
      if (scatterCount > 1) haplotypeCaller.scatterCount = scatterCount * 15
      haplotypeCaller.input_file = outputFiles("FinalBams")
      haplotypeCaller.out = new File(outputDir,sampleID + "/" + sampleID + ".gvcf.vcf")
      if (dbsnp != null) haplotypeCaller.dbsnp = dbsnp
      haplotypeCaller.nct = 3
      haplotypeCaller.memoryLimit = haplotypeCaller.nct * 2
      
      // GVCF options
      haplotypeCaller.emitRefConfidence = org.broadinstitute.sting.gatk.walkers.haplotypecaller.HaplotypeCaller.ReferenceConfidenceMode.GVCF
      haplotypeCaller.variant_index_type = GATKVCFIndexType.LINEAR
      haplotypeCaller.variant_index_parameter = 128000
      
      if (haplotypeCaller.input_file.size > 0) {
        add(haplotypeCaller)
        outputFiles += ("gvcf" -> List(haplotypeCaller.out))
      }
    } else {
      this.logger.warn("Sample in config missing ID, skipping sample")
    }
    return outputFiles
  }
  
  // Called for each run from a sample
  def runJobs(runConfig:Config,sampleConfig:Config) : Map[String,File] = {
    var outputFiles:Map[String,File] = Map()
    var paired: Boolean = false
    var runID: String = ""
    var fastq_R1: String = ""
    var fastq_R2: String = ""
    var sampleID: String = sampleConfig.get("ID").toString
    if (runConfig.contains("R1")) {
      fastq_R1 = runConfig.get("R1").toString
      if (runConfig.contains("R2")) {
        fastq_R2 = runConfig.get("R2").toString
        paired = true
      }
      if (runConfig.contains("ID")) runID = runConfig.get("ID").toString
      else throw new IllegalStateException("Missing ID on run for sample: " + sampleID)
      var runDir: String = outputDir + sampleID + "/run_" + runID + "/"
      
      val flexiprep = new Flexiprep(config)
      flexiprep.input_R1 = fastq_R1
      if (paired) flexiprep.input_R2 = fastq_R2
      flexiprep.outputDir = runDir + "flexiprep/"
      flexiprep.script
      addAll(flexiprep.functions) // Add function of flexiprep to curent function pool
      
      val bwaCommand = new Bwa(config.getAsConfig("bwa"))
      bwaCommand.R1 = flexiprep.outputFiles("output_R1")
      if (paired) bwaCommand.R2 = flexiprep.outputFiles("output_R2")
      bwaCommand.referenceFile = referenceFile
      bwaCommand.nCoresRequest = 8
      bwaCommand.jobResourceRequests :+= "h_vmem=6G"
      bwaCommand.RG = "@RG\\t" +
    		  "ID:" + sampleID + "_" + runID + "\\t" +
    		  "LB:" + sampleID + "_" + runID + "\\t" +
    		  "PL:illumina\\t" +
    		  "CN:SASC\\t" +
    		  "SM:" + sampleID + "\\t" +
    		  "PU:na"
      bwaCommand.output = new File(runDir + sampleID + "-run_" + runID + ".sam")
      add(bwaCommand)
      
      var bamFile:File = addSortSam(List(bwaCommand.output), swapExt(runDir,bwaCommand.output,".sam",".bam"), runDir)
      bamFile = addMarkDuplicates(List(bamFile), swapExt(runDir,bamFile,".bam",".dedup.bam"), runDir)
      bamFile = addIndelRealign(bamFile,runDir) // Indel realigner
      bamFile = addBaseRecalibrator(bamFile,runDir) // Base recalibrator
      
      outputFiles += ("FinalBam" -> bamFile)
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for runs: " + runConfig)    
    return outputFiles
  }
  
  def addSortSam(inputSam:List[File], outputFile:File, dir:String) : File = {
    val sortSam = new SortSam
    sortSam.input = inputSam
    sortSam.createIndex = true
    sortSam.output = outputFile
    sortSam.memoryLimit = 2
    sortSam.nCoresRequest = 2
    sortSam.jobResourceRequests :+= "h_vmem=4G"
    add(sortSam)
    
    return sortSam.output
  }
  
  def addMarkDuplicates(inputBams:List[File], outputFile:File, dir:String) : File = {
    val markDuplicates = new MarkDuplicates
    markDuplicates.input = inputBams
    markDuplicates.output = outputFile
    markDuplicates.REMOVE_DUPLICATES = false
    markDuplicates.metrics = swapExt(dir,outputFile,".bam",".metrics")
    markDuplicates.outputIndex = swapExt(dir,markDuplicates.output,".bam",".bai")
    markDuplicates.memoryLimit = 2
    markDuplicates.jobResourceRequests :+= "h_vmem=4G"
    add(markDuplicates)
    
    return markDuplicates.output
  }
  
  def addIndelRealign(inputBam:File, dir:String): File = {
    val realignerTargetCreator = new RealignerTargetCreator with gatkArguments
    realignerTargetCreator.I :+= inputBam
    realignerTargetCreator.o = swapExt(dir,inputBam,".bam",".realign.intervals")
    //realignerTargetCreator.nt = 1
    realignerTargetCreator.jobResourceRequests :+= "h_vmem=5G"
    if (scatterCount > 1) realignerTargetCreator.scatterCount = scatterCount
    add(realignerTargetCreator)

    val indelRealigner = new IndelRealigner with gatkArguments
    indelRealigner.I :+= inputBam
    indelRealigner.targetIntervals = realignerTargetCreator.o
    indelRealigner.o = swapExt(dir,inputBam,".bam",".realign.bam")
    if (scatterCount > 1) indelRealigner.scatterCount = scatterCount
    add(indelRealigner)
    
    return indelRealigner.o
  }
  
  def addBaseRecalibrator(inputBam:File, dir:String): File = {
    val baseRecalibrator = new BaseRecalibrator with gatkArguments
    baseRecalibrator.I :+= inputBam
    baseRecalibrator.o = swapExt(dir,inputBam,".bam",".baserecal")
    baseRecalibrator.knownSites :+= dbsnp
    if (scatterCount > 1) baseRecalibrator.scatterCount = scatterCount
    baseRecalibrator.nct = 2
    add(baseRecalibrator)

    val printReads = new PrintReads with gatkArguments
    printReads.I :+= inputBam
    printReads.o = swapExt(dir,inputBam,".bam",".baserecal.bam")
    printReads.BQSR = baseRecalibrator.o
    if (scatterCount > 1) printReads.scatterCount = scatterCount
    
    return printReads.o
  }
}
