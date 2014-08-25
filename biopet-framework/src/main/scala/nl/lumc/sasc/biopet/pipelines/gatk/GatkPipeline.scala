package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import java.io.File
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk._
import org.broadinstitute.gatk.queue.extensions.picard._
import org.broadinstitute.gatk.utils.commandline.{ Argument }

class GatkPipeline(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  @Argument(doc = "Only Sample", shortName = "sample", required = false)
  val onlySample: String = ""

  @Argument(doc = "Skip Genotyping step", shortName = "skipgenotyping", required = false)
  var skipGenotyping: Boolean = false

  @Argument(doc = "Merge gvcfs", shortName = "mergegvcfs", required = false)
  var mergeGvcfs: Boolean = false

  var reference: File = _
  var dbsnp: File = _
  var gvcfFiles: List[File] = Nil
  var finalBamFiles: List[File] = Nil

  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    reference = config("reference", required = true)
    dbsnp = config("dbsnp")
    if (configContains("gvcfFiles"))
      for (file <- config("gvcfFiles").getList)
        gvcfFiles :+= file.toString
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on gatk module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
  }

  def biopetScript() {
    if (onlySample.isEmpty) {
      runSamplesJobs

      //SampleWide jobs
      if (mergeGvcfs && gvcfFiles.size > 0) {
        val newFile = outputDir + "merged.gvcf.vcf"
        addCombineGVCFs(gvcfFiles, newFile)
        gvcfFiles = List(newFile)
      }

      if (!skipGenotyping && gvcfFiles.size > 0) {
        val gatkGenotyping = new GatkGenotyping(this)
        gatkGenotyping.inputGvcfs = gvcfFiles
        gatkGenotyping.outputDir = outputDir + "genotyping/"
        gatkGenotyping.init
        gatkGenotyping.biopetScript
        addAll(gatkGenotyping.functions)
        var vcfFile = gatkGenotyping.outputFile

        if (config("inputtype", default = "dna").getString != "rna") {
          vcfFile = addVariantAnnotator(vcfFile, finalBamFiles, outputDir + "recalibration/")
          vcfFile = addSnpVariantRecalibrator(vcfFile, outputDir + "recalibration/")
          vcfFile = addIndelVariantRecalibrator(vcfFile, outputDir + "recalibration/")
        }
      } else logger.warn("No gVCFs to genotype")
    } else runSingleSampleJobs(onlySample)
  }

  // Called for each sample
  def runSingleSampleJobs(sampleConfig: Map[String, Any]): Map[String, List[File]] = {
    var outputFiles: Map[String, List[File]] = Map()
    var libraryBamfiles: List[File] = List()
    var sampleID: String = sampleConfig("ID").toString
    for ((library, libraryFiles) <- runLibraryJobs(sampleConfig)) {
      libraryBamfiles +:= libraryFiles("FinalBam")
    }
    outputFiles += ("FinalBams" -> libraryBamfiles)

    if (libraryBamfiles.size > 0) {
      finalBamFiles ++= libraryBamfiles
      val gatkVariantcalling = new GatkVariantcalling(this)
      gatkVariantcalling.inputBams = libraryBamfiles
      gatkVariantcalling.outputDir = outputDir + sampleID + "/variantcalling/"
      gatkVariantcalling.init
      gatkVariantcalling.biopetScript
      addAll(gatkVariantcalling.functions)
      gvcfFiles :+= gatkVariantcalling.outputFile
    } else logger.warn("No bamfiles for variant calling for sample: " + sampleID)
    return outputFiles
  }

  // Called for each run from a sample
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): Map[String, File] = {
    var outputFiles: Map[String, File] = Map()
    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val runDir: String = outputDir + sampleID + "/run_" + runID + "/"
    var inputType = ""
    if (runConfig.contains("inputtype")) inputType = runConfig("inputtype").toString
    else inputType = config("inputtype", default = "dna").toString
    if (runConfig.contains("R1")) {
      val mapping = Mapping.loadFromLibraryConfig(this, runConfig, sampleConfig, runDir)
      addAll(mapping.functions) // Add functions of mapping to curent function pool

      outputFiles += ("FinalBam" -> mapping.outputFiles("finalBamFile"))
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for run: " + runConfig)
    return outputFiles
  }

  def addSnpVariantRecalibrator(inputVcf: File, dir: String): File = {
    val snpVariantRecalibrator = getVariantRecalibrator("snp")
    snpVariantRecalibrator.input +:= inputVcf
    snpVariantRecalibrator.recal_file = swapExt(dir, inputVcf, ".vcf", ".snp.recal")
    snpVariantRecalibrator.tranches_file = swapExt(dir, inputVcf, ".vcf", ".snp.tranches")
    if (!snpVariantRecalibrator.resource.isEmpty) {
      add(snpVariantRecalibrator)

      val snpApplyRecalibration = getApplyRecalibration("snp")
      snpApplyRecalibration.input +:= inputVcf
      snpApplyRecalibration.recal_file = snpVariantRecalibrator.recal_file
      snpApplyRecalibration.tranches_file = snpVariantRecalibrator.tranches_file
      snpApplyRecalibration.out = swapExt(dir, inputVcf, ".vcf", ".snp.recal.vcf")
      add(snpApplyRecalibration)

      return snpApplyRecalibration.out
    } else {
      logger.warn("Skipped snp Recalibration, resource is missing")
      return inputVcf
    }
  }

  def addIndelVariantRecalibrator(inputVcf: File, dir: String): File = {
    val indelVariantRecalibrator = getVariantRecalibrator("indel")
    indelVariantRecalibrator.input +:= inputVcf
    indelVariantRecalibrator.recal_file = swapExt(dir, inputVcf, ".vcf", ".indel.recal")
    indelVariantRecalibrator.tranches_file = swapExt(dir, inputVcf, ".vcf", ".indel.tranches")
    if (!indelVariantRecalibrator.resource.isEmpty) {
      add(indelVariantRecalibrator)

      val indelApplyRecalibration = getApplyRecalibration("indel")
      indelApplyRecalibration.input +:= inputVcf
      indelApplyRecalibration.recal_file = indelVariantRecalibrator.recal_file
      indelApplyRecalibration.tranches_file = indelVariantRecalibrator.tranches_file
      indelApplyRecalibration.out = swapExt(dir, inputVcf, ".vcf", ".indel.recal.vcf")
      add(indelApplyRecalibration)

      return indelApplyRecalibration.out
    } else {
      logger.warn("Skipped indel Recalibration, resource is missing")
      return inputVcf
    }
  }

  def getVariantRecalibrator(mode_arg: String): VariantRecalibrator = {
    val variantRecalibrator = new VariantRecalibrator() with gatkArguments {
      if (mode_arg == "indel") {
        this.mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
        if (configContains("mills", submodule = "variantrecalibrator"))
          this.resource :+= new TaggedFile(config("mills", submodule = "variantrecalibrator").getString, "known=false,training=true,truth=true,prior=12.0")
      } else { // SNP
        this.mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
        if (configContains("hapmap", submodule = "variantrecalibrator"))
          this.resource +:= new TaggedFile(config("hapmap", submodule = "variantrecalibrator").getString, "known=false,training=true,truth=true,prior=15.0")
        if (configContains("omni", submodule = "variantrecalibrator"))
          this.resource +:= new TaggedFile(config("omni", submodule = "variantrecalibrator").getString, "known=false,training=true,truth=true,prior=12.0")
        if (configContains("1000G", submodule = "variantrecalibrator"))
          this.resource +:= new TaggedFile(config("1000G", submodule = "variantrecalibrator").getString, "known=false,training=true,truth=false,prior=10.0")
      }
      if (configContains("dbsnp", submodule = "variantrecalibrator"))
        this.resource :+= new TaggedFile(config("dbsnp", submodule = "variantrecalibrator").getString, "known=true,training=false,truth=false,prior=2.0")
      this.nt = 4
      this.memoryLimit = nt * 2
      this.an = Seq("QD", "DP", "FS", "ReadPosRankSum", "MQRankSum")
      if (configContains("minnumbadvariants", submodule = "variantrecalibrator"))
        this.minNumBadVariants = config("minnumbadvariants", submodule = "variantrecalibrator")
      if (configContains("maxgaussians", submodule = "variantrecalibrator"))
        this.maxGaussians = config("maxgaussians", submodule = "variantrecalibrator")
    }
    return variantRecalibrator
  }

  def getApplyRecalibration(mode_arg: String): ApplyRecalibration = {
    val applyRecalibration = new ApplyRecalibration() with gatkArguments {
      if (mode_arg == "indel") {
        this.mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
        this.ts_filter_level = config("ts_filter_level", default = 99.0, submodule = "applyrecalibration")
      } else { // SNP
        this.mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
        this.ts_filter_level = config("ts_filter_level", default = 99.5, submodule = "applyrecalibration")
      }
      this.nt = 3
      this.memoryLimit = nt * 2
      if (configContains("scattercount", submodule = "applyrecalibration"))
        this.scatterCount = config("scattercount", submodule = "applyrecalibration")
    }
    return applyRecalibration
  }

  def addVariantAnnotator(inputvcf: File, bamfiles: List[File], dir: String): File = {
    val variantAnnotator = new VariantAnnotator with gatkArguments {
      this.variant = inputvcf
      this.input_file = bamfiles
      this.dbsnp = config("dbsnp", submodule = "variantannotator")
      this.out = swapExt(dir, inputvcf, ".vcf", ".anotated.vcf")
      if (configContains("scattercount", submodule = "variantannotator"))
        this.scatterCount = config("scattercount", submodule = "variantannotator")
    }
    add(variantAnnotator)

    return variantAnnotator.out
  }

  def addCombineGVCFs(input: List[File], output: File): File = {
    val combineGVCFs = new CombineGVCFs with gatkArguments {
      this.variant = input
      this.o = output
      if (configContains("scattercount", submodule = "variantannotator"))
        this.scatterCount = config("scattercount", submodule = "combinegvcfs")
    }
    add(combineGVCFs)

    return output
  }

  trait gatkArguments extends CommandLineGATK {
    this.reference_sequence = reference
    this.memoryLimit = 2
    this.jobResourceRequests :+= "h_vmem=4G"
  }
}

object GatkPipeline extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/GatkPipeline.class"
}
