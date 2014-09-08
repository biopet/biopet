package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import java.io.File
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

  var reference: File = _
  var dbsnp: File = _
  var gvcfFiles: List[File] = Nil
  var finalBamFiles: List[File] = Nil

  override def init() {
    super.init
    reference = config("reference", required = true)
    dbsnp = config("dbsnp")
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
      gatkVariantcalling.outputDir = globalSampleDir + sampleID + "/variantcalling/"
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
    val runDir: String = globalSampleDir + sampleID + "/run_" + runID + "/"
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
}

object GatkPipeline extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/gatk/GatkPipeline.class"
}
