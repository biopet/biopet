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
  qscript =>
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
        val genotypeGVCFs = new GenotypeGVCFs() with gatkArguments {
          this.variant = qscript.gvcfFiles
          this.scatterCount = qscript.scatterCount
          this.out = new File(outputDir,"final.vcf")
        }
        add(genotypeGVCFs)
        
        //Snp recal
        val snpVariantRecalibrator = new VariantRecalibrator() with gatkArguments {
          this.input +:= genotypeGVCFs.out
          this.nt = 4
          this.memoryLimit = 2 * nt
          this.recal_file = swapExt(genotypeGVCFs.out,".vcf",".snp.recal")
          this.tranches_file = swapExt(genotypeGVCFs.out,".vcf",".snp.tranches")
          this.resource = Seq(new TaggedFile(config.getAsString("hapmap"), "known=false,training=true,truth=true,prior=15.0"),
                              new TaggedFile(config.getAsString("omni"), "known=false,training=true,truth=true,prior=12.0"),
                              new TaggedFile(config.getAsString("1000G"), "known=false,training=true,truth=false,prior=10.0"),
                              new TaggedFile(config.getAsString("dbsnp"), "known=true,training=false,truth=false,prior=2.0"))
          this.an = Seq("QD","MQ","MQRankSum","ReadPosRankSum","FS","DP","InbreedingCoeff")
          this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
        }
        add(snpVariantRecalibrator)
        
        val snpApplyRecalibration = new ApplyRecalibration() with gatkArguments {
          this.input +:= genotypeGVCFs.out
          this.recal_file = snpVariantRecalibrator.recal_file
          this.tranches_file = snpVariantRecalibrator.tranches_file
          this.out = swapExt(genotypeGVCFs.out,".vcf",".snp.recal.vcf")
          this.ts_filter_level = 99.5
          this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
          this.nt = 3
          this.memoryLimit = 2 * nt
          if (scatterCount > 1) this.scatterCount = qscript.scatterCount
        }
        add(snpApplyRecalibration)
        
        //indel recal
        val indelVariantRecalibrator = new VariantRecalibrator() with gatkArguments {
          this.input +:= snpApplyRecalibration.out
          this.nt = 4
          this.memoryLimit = 2 * nt
          this.recal_file = swapExt(genotypeGVCFs.out,".vcf",".indel.recal")
          this.tranches_file = swapExt(genotypeGVCFs.out,".vcf",".indel.tranches")
          this.resource :+= new TaggedFile(config.getAsString("mills"), "known=false,training=true,truth=true,prior=12.0")
          this.resource :+= new TaggedFile(config.getAsString("dbsnp"), "known=true,training=false,truth=false,prior=2.0")
          this.an = Seq("QD","DP","FS","ReadPosRankSum","MQRankSum","InbreedingCoeff")
          this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
        }
        add(indelVariantRecalibrator)
        
        val indelApplyRecalibration = new ApplyRecalibration() with gatkArguments {
          this.input +:= snpApplyRecalibration.out
          this.recal_file = indelVariantRecalibrator.recal_file
          this.tranches_file = indelVariantRecalibrator.tranches_file
          this.out = swapExt(genotypeGVCFs.out,".recal.vcf",".indel.recal.vcf")
          this.ts_filter_level = 99.0
          this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
          this.nt = 3
          this.memoryLimit = 2 * nt
          if (scatterCount > 1) this.scatterCount = qscript.scatterCount
        }
        add(indelApplyRecalibration)
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
      if (scatterCount > 1) haplotypeCaller.scatterCount = qscript.scatterCount * 15
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
      addAll(flexiprep.functions) // Add functions of flexiprep to curent function pool
      
      val bwaCommand = new Bwa(config)
      bwaCommand.R1 = flexiprep.outputFiles("output_R1")
      if (paired) bwaCommand.R2 = flexiprep.outputFiles("output_R2")
      bwaCommand.referenceFile = qscript.referenceFile
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
      if (scatterCount > 1) this.scatterCount = scatterCount
    }
    add(realignerTargetCreator)

    val indelRealigner = new IndelRealigner with gatkArguments {
      this.I :+= inputBam
      this.targetIntervals = realignerTargetCreator.o
      this.o = swapExt(dir,inputBam,".bam",".realign.bam")
      if (scatterCount > 1) this.scatterCount = scatterCount
    }
    add(indelRealigner)
    
    return indelRealigner.o
  }
  
  def addBaseRecalibrator(inputBam:File, dir:String): File = {
    val baseRecalibrator = new BaseRecalibrator with gatkArguments {
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".baserecal")
      this.knownSites :+= dbsnp
      if (scatterCount > 1) this.scatterCount = scatterCount
      this.nct = 2
    }
    add(baseRecalibrator)

    val printReads = new PrintReads with gatkArguments {
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".baserecal.bam")
      this.BQSR = baseRecalibrator.o
      if (scatterCount > 1) this.scatterCount = scatterCount
    }
    
    return printReads.o
  }
}
