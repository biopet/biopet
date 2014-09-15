package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import htsjdk.samtools.SAMFileReader
import scala.collection.JavaConversions._
import java.io.File
import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.gatk.HaplotypeCaller
import nl.lumc.sasc.biopet.extensions.picard.AddOrReplaceReadGroups
import nl.lumc.sasc.biopet.extensions.gatk.CombineGVCFs
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Argument }

class GatkPipeline(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  @Argument(doc = "Only Sample", shortName = "sample", required = false)
  val onlySample: String = ""

  @Argument(doc = "Skip Genotyping step", shortName = "skipgenotyping", required = false)
  var skipGenotyping: Boolean = false

  @Argument(doc = "Merge gvcfs", shortName = "mergegvcfs", required = false)
  var mergeGvcfs: Boolean = false

  @Argument(doc = "Joint variantcalling", shortName = "jointCalling", required = false)
  var jointVariantcalling = false
  
  var reference: File = _
  var dbsnp: File = _
  var gvcfFiles: List[File] = Nil
  var finalBamFiles: List[File] = Nil

  class LibraryOutput extends AbstractLibraryOutput {
    var mappedBamFile: File = _
    var variantcalling: GatkVariantcalling.ScriptOutput = _
  }
  
  class SampleOutput extends AbstractSampleOutput {
    var variantcalling: GatkVariantcalling.ScriptOutput = _
  }
  
  def init() {
    reference = config("reference", required = true)
    dbsnp = config("dbsnp")
    if (config.contains("target_bed")) {
      defaults ++= Map("gatk" -> Map(("intervals" -> config("target_bed").getStringList)))
    }
    if (config.contains("joint_variantcalling")) jointVariantcalling = config("joint_variantcalling", default = false)
    if (config.contains("gvcfFiles"))
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
        add(CombineGVCFs(this, gvcfFiles, newFile))
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
          val recalibration = new GatkVariantRecalibration(this)
          recalibration.inputVcf = vcfFile
          recalibration.bamFiles = finalBamFiles
          recalibration.outputDir = outputDir + "recalibration/"
          recalibration.init
          recalibration.biopetScript
          vcfFile = recalibration.outputVcf
        }
      } else logger.warn("No gVCFs to genotype")
      
      if (jointVariantcalling) {
        val allBamfiles = for ((sampleID,sampleOutput) <- samplesOutput;
                                file <- sampleOutput.variantcalling.bamFiles) yield file
        val allRawVcfFiles = for ((sampleID,sampleOutput) <- samplesOutput) yield sampleOutput.variantcalling.rawVcfFile
        val hcDiscorvery = new HaplotypeCaller(this)
        hcDiscorvery.input_file = allBamfiles.toSeq
        hcDiscorvery.scatterCount = config("scattercount", submodule = "multisample")
        hcDiscorvery.out = outputDir + "variantcalling/hc.vcf.gz"
        add(hcDiscorvery)

        val cvRaw = CombineVariants(this, allRawVcfFiles.toList, outputDir + "variantcalling/raw.vcf.gz")
        add(cvRaw)

        val hcRaw = new HaplotypeCaller(this)
        hcRaw.input_file = allBamfiles.toSeq
        hcRaw.scatterCount = config("scattercount", submodule = "multisample")
        hcRaw.out = outputDir + "variantcalling/raw_genotype.vcf.gz"
        hcRaw.alleles = cvRaw.out
        hcRaw.genotyping_mode = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypingOutputMode.GENOTYPE_GIVEN_ALLELES
        add(hcRaw)

        val cvFinal = CombineVariants(this, List(hcDiscorvery.out, hcRaw.out), outputDir + "variantcalling/merge.vcf.gz")
        add(cvFinal)
      }
    } else runSingleSampleJobs(onlySample)
  }

  // Called for each sample
  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {
    val sampleOutput = new SampleOutput
    var libraryBamfiles: List[File] = List()
    var sampleID: String = sampleConfig("ID").toString
    sampleOutput.libraries = runLibraryJobs(sampleConfig)
    for ((libraryID, libraryOutput) <- sampleOutput.libraries) {
      libraryBamfiles ++= libraryOutput.variantcalling.bamFiles
    }

    if (libraryBamfiles.size > 0) {
      finalBamFiles ++= libraryBamfiles
      val gatkVariantcalling = new GatkVariantcalling(this)
      gatkVariantcalling.inputBams = libraryBamfiles
      gatkVariantcalling.outputDir = globalSampleDir + sampleID + "/variantcalling/"
      gatkVariantcalling.preProcesBams = false
      gatkVariantcalling.sampleID = sampleID
      gatkVariantcalling.init
      gatkVariantcalling.biopetScript
      addAll(gatkVariantcalling.functions)
      sampleOutput.variantcalling = gatkVariantcalling.scriptOutput
      gvcfFiles :+= gatkVariantcalling.scriptOutput.gvcfFile
    } else logger.warn("No bamfiles for variant calling for sample: " + sampleID)
    return sampleOutput
  }

  // Called for each run from a sample
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {
    val libraryOutput = new LibraryOutput
    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val runDir: String = globalSampleDir + sampleID + "/run_" + runID + "/"
    var inputType = ""
    if (runConfig.contains("inputtype")) inputType = runConfig("inputtype").toString
    else inputType = config("inputtype", default = "dna").toString
    if (runConfig.contains("R1")) {
      val mapping = Mapping.loadFromLibraryConfig(this, runConfig, sampleConfig, runDir)
      addAll(mapping.functions) // Add functions of mapping to curent function pool
      libraryOutput.mappedBamFile = mapping.outputFiles("finalBamFile")
    } else if (runConfig.contains("bam")) {
      var bamFile = new File(runConfig("bam").toString)
      if (!bamFile.exists) throw new IllegalStateException("Bam in config does not exist, file: " + bamFile)
      
      var readGroupOke = true
      val inputSam = new SAMFileReader(bamFile)
      val header = inputSam.getFileHeader.getReadGroups
      for (readGroup <- inputSam.getFileHeader.getReadGroups) {
        if (readGroup.getSample != sampleID) logger.warn("Sample ID readgroup in bam file is not the same")
        if (readGroup.getLibrary != runID) logger.warn("Library ID readgroup in bam file is not the same")
        if (readGroup.getSample != sampleID || readGroup.getLibrary != runID) readGroupOke = false
      }
      inputSam.close
      
      if (!readGroupOke) {
        if (config("correct_readgroups", default = false)) {
          logger.info("Correcting readgroups, file:" + bamFile)
          val aorrg = AddOrReplaceReadGroups(this, bamFile, new File(runDir + sampleID + "-" + runID + ".bam"))
          aorrg.RGID = sampleID + "-" + runID
          aorrg.RGLB = runID
          aorrg.RGSM = sampleID
          if (runConfig.contains("PL")) aorrg.RGPL = runConfig("PL").toString
          else aorrg.RGPL = "illumina"
          if (runConfig.contains("PU")) aorrg.RGPU = runConfig("PU").toString
          else aorrg.RGPU = "na"
          if (runConfig.contains("CN")) aorrg.RGCN = runConfig("CN").toString
          add(aorrg, isIntermediate = true)
          bamFile = aorrg.output
        } else throw new IllegalStateException("Readgroup sample and/or library of input bamfile is not correct, file: " + bamFile + 
            "\nPossible to set 'correct_readgroups' to true on config to automatic fix this")
      }
      
      libraryOutput.mappedBamFile = bamFile
    } else logger.error("Sample: " + sampleID + ": No R1 found for run: " + runConfig)
    
    val gatkVariantcalling = new GatkVariantcalling(this)
    gatkVariantcalling.inputBams = List(libraryOutput.mappedBamFile)
    gatkVariantcalling.outputDir = runDir
    gatkVariantcalling.variantcalling = config("library_variantcalling", default = false)
    gatkVariantcalling.preProcesBams = true
    gatkVariantcalling.sampleID = sampleID
    gatkVariantcalling.init
    gatkVariantcalling.biopetScript
    addAll(gatkVariantcalling.functions)
    libraryOutput.variantcalling = gatkVariantcalling.scriptOutput
    //libraryOutput.preProcesBamFile = gatkVariantcalling.outputFiles("final_bam")
    
    return libraryOutput
  }
}

object GatkPipeline extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/GatkPipeline.class"  
}
