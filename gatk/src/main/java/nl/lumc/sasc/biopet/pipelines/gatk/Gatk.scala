package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.wrappers._
import nl.lumc.sasc.biopet.wrappers.aligners._
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.pipelines.mapping._
import nl.lumc.sasc.biopet.pipelines.flexiprep._
import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.extensions.picard._
import org.broadinstitute.sting.queue.function._
import scala.util.parsing.json._
import org.broadinstitute.sting.utils.variant._

class Gatk(private var globalConfig: Config) extends QScript with BiopetQScript {
  qscript =>
  def this() = this(new Config())
  
  @Argument(doc="Config Json file",shortName="config") var configfiles: List[File] = Nil
  @Argument(doc="Only Sample",shortName="sample", required=false) var onlySample: String = ""
  @Argument(doc="Output directory", shortName="outputDir", required=true) var outputDir: String = _
  
  //var config: Config = _
  var referenceFile: File = _
  var dbsnp: File = _
  var gvcfFiles: List[File] = Nil
  
  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    config = Config.mergeConfigs(globalConfig.getAsConfig("gatk"), globalConfig)
    referenceFile = config.getAsString("referenceFile")
    dbsnp = config.getAsString("dbsnp")
    gvcfFiles = config.getAsListOfStrings("gvcfFiles", Nil)
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on flexiprep module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }
  
  def script() {
    init()
    if (onlySample.isEmpty) {
      runSamplesJobs
      
      //SampleWide jobs
      if (gvcfFiles.size > 0) {
        var vcfFile = addGenotypeGVCFs(gvcfFiles, outputDir + "recalibration/")
        vcfFile = addSnpVariantRecalibrator(vcfFile, outputDir + "recalibration/")
        vcfFile = addIndelVariantRecalibrator(vcfFile, outputDir + "recalibration/")
      } else logger.warn("No gVCFs to genotype")
    } else runSingleSampleJobs(onlySample)
  }
  
  // Called for each sample
  override def runSingleSampleJobs(sampleConfig:Config) : Map[String,List[File]] = {
    var outputFiles:Map[String,List[File]] = Map()
    var runBamfiles: List[File] = List()
    var sampleID: String = sampleConfig.getAsString("ID")
    for ((run, runFiles) <- runRunsJobs(sampleConfig)) {
      runBamfiles +:= runFiles("FinalBam")
    }
    outputFiles += ("FinalBams" -> runBamfiles)
    
    if (runBamfiles.size > 0) {
      var gvcfFile = addHaplotypeCaller(runBamfiles, new File(outputDir + sampleID + "/" + sampleID + ".gvcf.vcf"))
      outputFiles += ("gvcf" -> List(gvcfFile))
      gvcfFiles :+= gvcfFile
    } else logger.warn("No bamfiles for variant calling for sample: " + sampleID)
    return outputFiles
  }
  
  // Called for each run from a sample
  override def runSingleRunJobs(runConfig:Config, sampleConfig:Config) : Map[String,File] = {
    var outputFiles:Map[String,File] = Map()
    val runID: String = runConfig.getAsString("ID")
    val sampleID: String = sampleConfig.get("ID").toString
    val runDir: String = outputDir + sampleID + "/run_" + runID + "/"
    if (runConfig.contains("R1")) {
      val mapping = new Mapping(config)
      mapping.loadRunConfig(runConfig, sampleConfig, runDir)
      mapping.script
      addAll(mapping.functions) // Add functions of mapping to curent function pool
      
      var bamFile:File = addIndelRealign(mapping.outputFiles("finalBamFile"),runDir) // Indel realigner
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
      val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("realignertargetcreator"), qscript.config)
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".realign.intervals")
      this.jobResourceRequests :+= "h_vmem=5G"
      if (config.contains("scattercount")) this.scatterCount = config.getAsInt("scattercount")
    }
    add(realignerTargetCreator)

    val indelRealigner = new IndelRealigner with gatkArguments {
      val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("indelrealigner"), qscript.config)
      this.I :+= inputBam
      this.targetIntervals = realignerTargetCreator.o
      this.o = swapExt(dir,inputBam,".bam",".realign.bam")
      if (config.contains("scattercount")) this.scatterCount = config.getAsInt("scattercount")
    }
    add(indelRealigner)
    
    return indelRealigner.o
  }
  
  def addBaseRecalibrator(inputBam:File, dir:String): File = {
    val baseRecalibrator = new BaseRecalibrator with gatkArguments {
      val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("baserecalibrator"), qscript.config)
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".baserecal")
      this.knownSites :+= dbsnp
      if (config.contains("scattercount")) this.scatterCount = config.getAsInt("scattercount")
      this.nct = 2
    }
    add(baseRecalibrator)

    val printReads = new PrintReads with gatkArguments {
      val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("printreads"), qscript.config)
      this.I :+= inputBam
      this.o = swapExt(dir,inputBam,".bam",".baserecal.bam")
      this.BQSR = baseRecalibrator.o
      if (config.contains("scattercount")) this.scatterCount = config.getAsInt("scattercount")
    }
    
    return printReads.o
  }
  
  def addHaplotypeCaller(bamfiles:List[File], outputfile:File): File = {
    val haplotypeCaller = new HaplotypeCaller with gatkArguments {
      val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("haplotypecaller"), qscript.config)
      if (config.contains("scattercount")) this.scatterCount = config.getAsInt("scattercount")
      this.input_file = bamfiles
      this.out = outputfile
      if (dbsnp != null) this.dbsnp = qscript.dbsnp
      this.nct = 3
      this.memoryLimit = this.nct * 2
      
      // GVCF options
      this.emitRefConfidence = org.broadinstitute.sting.gatk.walkers.haplotypecaller.HaplotypeCaller.ReferenceConfidenceMode.GVCF
      this.variant_index_type = GATKVCFIndexType.LINEAR
      this.variant_index_parameter = 128000
    }
    add(haplotypeCaller)
    
    return haplotypeCaller.out
  }
  
  def addSnpVariantRecalibrator(inputVcf:File, dir:String): File = {
    val snpVariantRecalibrator = new VariantRecalibrator() with gatkArguments {
      val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("variantrecalibrator"), qscript.config)
      this.input +:= inputVcf
      this.nt = 4
      this.memoryLimit = 2 * nt
      this.recal_file = swapExt(dir, inputVcf,".vcf",".snp.recal")
      this.tranches_file = swapExt(dir, inputVcf,".vcf",".snp.tranches")
      this.resource = Seq(new TaggedFile(config.getAsString("hapmap"), "known=false,training=true,truth=true,prior=15.0"),
                          new TaggedFile(config.getAsString("omni"), "known=false,training=true,truth=true,prior=12.0"),
                          new TaggedFile(config.getAsString("1000G"), "known=false,training=true,truth=false,prior=10.0"),
                          new TaggedFile(config.getAsString("dbsnp"), "known=true,training=false,truth=false,prior=2.0"))
      this.an = Seq("QD","MQ","MQRankSum","ReadPosRankSum","FS","DP","InbreedingCoeff")
      this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
    }
    add(snpVariantRecalibrator)

    val snpApplyRecalibration = new ApplyRecalibration() with gatkArguments {
      val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("applyrecalibration"), qscript.config)
      this.input +:= inputVcf
      this.recal_file = snpVariantRecalibrator.recal_file
      this.tranches_file = snpVariantRecalibrator.tranches_file
      this.out = swapExt(dir, inputVcf,".vcf",".snp.recal.vcf")
      this.ts_filter_level = 99.5
      this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
      this.nt = 3
      this.memoryLimit = 2 * nt
      if (config.contains("scattercount")) this.scatterCount = config.getAsInt("scattercount")
    }
    add(snpApplyRecalibration)
    
    return snpApplyRecalibration.out
  }
  
  def addIndelVariantRecalibrator(inputVcf:File, dir:String): File = {
    val indelVariantRecalibrator = new VariantRecalibrator() with gatkArguments {
      val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("variantrecalibrator"), qscript.config)
      this.input +:= inputVcf
      this.nt = 4
      this.memoryLimit = 2 * nt
      this.recal_file = swapExt(dir, inputVcf,".vcf",".indel.recal")
      this.tranches_file = swapExt(dir, inputVcf,".vcf",".indel.tranches")
      this.resource :+= new TaggedFile(config.getAsString("mills"), "known=false,training=true,truth=true,prior=12.0")
      this.resource :+= new TaggedFile(config.getAsString("dbsnp"), "known=true,training=false,truth=false,prior=2.0")
      this.an = Seq("QD","DP","FS","ReadPosRankSum","MQRankSum","InbreedingCoeff")
      this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
    }
    add(indelVariantRecalibrator)

    val indelApplyRecalibration = new ApplyRecalibration() with gatkArguments {
      val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("applyrecalibration"), qscript.config)
      this.input +:= inputVcf
      this.recal_file = indelVariantRecalibrator.recal_file
      this.tranches_file = indelVariantRecalibrator.tranches_file
      this.out = swapExt(dir, inputVcf,".vcf",".indel.recal.vcf")
      this.ts_filter_level = 99.0
      this.mode = org.broadinstitute.sting.gatk.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
      this.nt = 3
      this.memoryLimit = 2 * nt
      if (config.contains("scattercount")) this.scatterCount = config.getAsInt("scattercount")
    }
    add(indelApplyRecalibration)
    
    return indelApplyRecalibration.out
  }
  
  def addGenotypeGVCFs(gvcfFiles: List[File], dir:String): File = {
    val genotypeGVCFs = new GenotypeGVCFs() with gatkArguments {
      val config: Config = Config.mergeConfigs(qscript.config.getAsConfig("genotypegvcfs"), qscript.config)
      this.variant = gvcfFiles
      if (config.contains("scattercount")) this.scatterCount = config.getAsInt("scattercount")
      this.out = new File(outputDir,"genotype.vcf")
    }
    add(genotypeGVCFs)
    return genotypeGVCFs.out
  }
  
  trait gatkArguments extends CommandLineGATK {
    this.reference_sequence = referenceFile
    this.memoryLimit = 2
    this.jobResourceRequests :+= "h_vmem=4G"
  }
}

object Gatk extends PipelineCommand {
  override val src = "Gatk"
}
