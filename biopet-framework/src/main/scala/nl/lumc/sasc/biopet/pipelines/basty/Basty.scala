package nl.lumc.sasc.biopet.pipelines.basty

import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Cat
import nl.lumc.sasc.biopet.pipelines.gatk.GatkPipeline
import nl.lumc.sasc.biopet.tools.BastyGenerateFasta
import org.broadinstitute.gatk.queue.QScript

class Basty(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  class LibraryOutput extends AbstractLibraryOutput {
  }

  class SampleOutput extends AbstractSampleOutput {
    var outputVariants: File = _
    var outputVariantsSnps: File = _
  }

  defaults ++= Map("ploidy" -> 1, "use_haplotypecaller" -> false, "use_unifiedgenotyper" -> true)

  var gatkPipeline: GatkPipeline = _

  def init() {
    gatkPipeline = new GatkPipeline(this)
    gatkPipeline.outputDir = outputDir
    gatkPipeline.init
  }

  def biopetScript() {
    gatkPipeline.biopetScript
    addAll(gatkPipeline.functions)

    val refVariants = addGenerateFasta("Reference", outputDir + "reference/")
    val refVariantSnps = addGenerateFasta("Reference", outputDir + "reference/", snpsOnly = true)

    runSamplesJobs()

    add(Cat(this, refVariants :: samplesOutput.map(_._2.outputVariants).toList, outputDir + "fastas/variant.fasta"))
    add(Cat(this, refVariantSnps :: samplesOutput.map(_._2.outputVariantsSnps).toList, outputDir + "fastas/variant.snps_only.fasta"))
  }

  // Called for each sample
  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {
    val sampleOutput = new SampleOutput
    val sampleID: String = sampleConfig("ID").toString
    val sampleDir = globalSampleDir + sampleID + "/"

    sampleOutput.libraries = runLibraryJobs(sampleConfig)

    sampleOutput.outputVariants = addGenerateFasta(sampleID, sampleDir)
    sampleOutput.outputVariantsSnps = addGenerateFasta(sampleID, sampleDir, snpsOnly = true)

    return sampleOutput
  }

  // Called for each run from a sample
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {
    val libraryOutput = new LibraryOutput

    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val runDir: String = globalSampleDir + sampleID + "/run_" + runID + "/"

    return libraryOutput
  }

  def addGenerateFasta(sampleName: String, outputDir: String, snpsOnly: Boolean = false): File = {
    val bastyGenerateFasta = new BastyGenerateFasta(this)
    bastyGenerateFasta.inputVcf = gatkPipeline.multisampleVariantcalling.scriptOutput.finalVcfFile
    bastyGenerateFasta.outputVariants = outputDir + sampleName + ".variants" + (if (snpsOnly) ".snps_only" else "") + ".fasta"
    bastyGenerateFasta.sampleName = sampleName
    bastyGenerateFasta.snpsOnly = snpsOnly
    add(bastyGenerateFasta)
    return bastyGenerateFasta.outputVariants
  }
}

object Basty extends PipelineCommand
