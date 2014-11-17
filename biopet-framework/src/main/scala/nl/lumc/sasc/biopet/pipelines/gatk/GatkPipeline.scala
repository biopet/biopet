package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import htsjdk.samtools.SamReaderFactory
import scala.collection.JavaConversions._
import java.io.File
import nl.lumc.sasc.biopet.extensions.gatk.{ CombineVariants, CombineGVCFs }
import nl.lumc.sasc.biopet.extensions.picard.AddOrReplaceReadGroups
import nl.lumc.sasc.biopet.extensions.picard.SamToFastq
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Argument }

class GatkPipeline(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  @Argument(doc = "Only Sample", shortName = "sample", required = false)
  val onlySample: List[String] = Nil

  @Argument(doc = "Skip Genotyping step", shortName = "skipgenotyping", required = false)
  var skipGenotyping: Boolean = false

  @Argument(doc = "Merge gvcfs", shortName = "mergegvcfs", required = false)
  var mergeGvcfs: Boolean = false

  @Argument(doc = "Joint variantcalling", shortName = "jointVariantCalling", required = false)
  var jointVariantcalling: Boolean = config("joint_variantcalling", default = false)

  @Argument(doc = "Joint genotyping", shortName = "jointGenotyping", required = false)
  var jointGenotyping: Boolean = config("joint_genotyping", default = false)

  var singleSampleCalling = config("single_sample_calling", default = true)
  var reference: File = config("reference", required = true)
  var dbsnp: File = config("dbsnp")
  var gvcfFiles: List[File] = Nil
  var finalBamFiles: List[File] = Nil
  var useAllelesOption: Boolean = config("use_alleles_option", default = false)

  class LibraryOutput extends AbstractLibraryOutput {
    var mappedBamFile: File = _
    var variantcalling: GatkVariantcalling.ScriptOutput = _
  }

  class SampleOutput extends AbstractSampleOutput {
    var variantcalling: GatkVariantcalling.ScriptOutput = _
  }

  def init() {
    if (config.contains("target_bed")) {
      defaults ++= Map("gatk" -> Map(("intervals" -> config("target_bed").getStringList)))
    }
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
        val newFile = outputDir + "merged.gvcf.vcf.gz"
        add(CombineGVCFs(this, gvcfFiles, newFile))
        gvcfFiles = List(newFile)
      }

      if (!skipGenotyping && gvcfFiles.size > 0) {
        if (jointGenotyping) {
          val gatkGenotyping = new GatkGenotyping(this)
          gatkGenotyping.inputGvcfs = gvcfFiles
          gatkGenotyping.outputDir = outputDir + "genotyping/"
          gatkGenotyping.init
          gatkGenotyping.biopetScript
          addAll(gatkGenotyping.functions)
          var vcfFile = gatkGenotyping.outputFile
        }
      } else logger.warn("No gVCFs to genotype")

      if (jointVariantcalling) {
        val allBamfiles = for (
          (sampleID, sampleOutput) <- samplesOutput;
          file <- sampleOutput.variantcalling.bamFiles
        ) yield file
        val allRawVcfFiles = for ((sampleID, sampleOutput) <- samplesOutput) yield sampleOutput.variantcalling.rawFilterVcfFile

        val gatkVariantcalling = new GatkVariantcalling(this) {
          override protected lazy val configName = "gatkvariantcalling"
          override def configPath: List[String] = "multisample" :: super.configPath
        }

        if (gatkVariantcalling.useMpileup) {
          val cvRaw = CombineVariants(this, allRawVcfFiles.toList, outputDir + "variantcalling/multisample.raw.vcf.gz")
          add(cvRaw)
          gatkVariantcalling.rawVcfInput = cvRaw.out
        }

        gatkVariantcalling.preProcesBams = Some(false)
        gatkVariantcalling.doublePreProces = Some(false)
        gatkVariantcalling.inputBams = allBamfiles.toList
        gatkVariantcalling.outputDir = outputDir + "variantcalling"
        gatkVariantcalling.outputName = "multisample"
        gatkVariantcalling.init
        gatkVariantcalling.biopetScript
        addAll(gatkVariantcalling.functions)

        if (config("inputtype", default = "dna").getString != "rna" && config("recalibration", default = false).getBoolean) {
          val recalibration = new GatkVariantRecalibration(this)
          recalibration.inputVcf = gatkVariantcalling.scriptOutput.finalVcfFile
          recalibration.bamFiles = finalBamFiles
          recalibration.outputDir = outputDir + "recalibration/"
          recalibration.init
          recalibration.biopetScript
        }
      }
    } else for (sample <- onlySample) runSingleSampleJobs(sample)
  }

  // Called for each sample
  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {
    val sampleOutput = new SampleOutput
    var libraryBamfiles: List[File] = List()
    val sampleID: String = sampleConfig("ID").toString
    sampleOutput.libraries = runLibraryJobs(sampleConfig)
    val sampleDir = globalSampleDir + sampleID
    for ((libraryID, libraryOutput) <- sampleOutput.libraries) {
      libraryBamfiles ++= libraryOutput.variantcalling.bamFiles
    }

    if (libraryBamfiles.size > 0) {
      finalBamFiles ++= libraryBamfiles
      val gatkVariantcalling = new GatkVariantcalling(this)
      gatkVariantcalling.inputBams = libraryBamfiles
      gatkVariantcalling.outputDir = sampleDir + "/variantcalling/"
      gatkVariantcalling.preProcesBams = Some(false)
      if (!singleSampleCalling) {
        gatkVariantcalling.useHaplotypecaller = Some(false)
        gatkVariantcalling.useUnifiedGenotyper = Some(false)
      }
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

      if (config("bam_to_fastq", default = false).getBoolean) {
        val samToFastq = SamToFastq(this, bamFile, runDir + sampleID + "-" + runID + ".R1.fastq",
          runDir + sampleID + "-" + runID + ".R2.fastq")
        add(samToFastq, isIntermediate = true)
        val mapping = Mapping.loadFromLibraryConfig(this, runConfig, sampleConfig, runDir, startJobs = false)
        mapping.input_R1 = samToFastq.fastqR1
        mapping.input_R2 = samToFastq.fastqR2
        mapping.init
        mapping.biopetScript
        addAll(mapping.functions) // Add functions of mapping to curent function pool
        libraryOutput.mappedBamFile = mapping.outputFiles("finalBamFile")
      } else {
        var readGroupOke = true
        val inputSam = SamReaderFactory.makeDefault.open(bamFile)
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
        addAll(BamMetrics(this, bamFile, runDir + "metrics/").functions)

        libraryOutput.mappedBamFile = bamFile
      }
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

    return libraryOutput
  }
}

object GatkPipeline extends PipelineCommand
