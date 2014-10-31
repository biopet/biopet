package nl.lumc.sasc.biopet.pipelines.basty

import java.io.File
import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Cat
import nl.lumc.sasc.biopet.extensions.Raxml
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

    val catVariants = Cat(this, refVariants :: samplesOutput.map(_._2.outputVariants).toList, outputDir + "fastas/variant.fasta")
    add(catVariants)
    val catVariantsSnps = Cat(this, refVariantSnps :: samplesOutput.map(_._2.outputVariantsSnps).toList, outputDir + "fastas/variant.snps_only.fasta")
    add(catVariantsSnps)

    val seed = scala.util.Random.nextInt
    def addRaxml(input: File, outputDir: String, outputName: String) {
      val raxmlMl = new Raxml(this)
      raxmlMl.input = input
      raxmlMl.m = config("raxml_ml_model", default = "GTRGAMMAX")
      raxmlMl.p = config("seed", default = seed)
      raxmlMl.n = outputName + "_ml"
      raxmlMl.w = outputDir
      raxmlMl.N = config("ml_runs", default = 20, submodule = "raxml")
      add(raxmlMl)

      val raxmlBoot = new Raxml(this)
      raxmlMl.input = input
      raxmlBoot.m = config("raxml_ml_model", default = "GTRGAMMAX")
      raxmlBoot.p = config("seed", default = seed)
      raxmlBoot.b = config("seed", default = seed)
      raxmlBoot.w = outputDir
      raxmlBoot.n = outputName + "_boot"
      add(raxmlBoot)

      val raxmlBi = new Raxml(this)
      raxmlMl.input = input
      raxmlBi.t = raxmlMl.getBestTree
      raxmlBi.z = raxmlBoot.getBootstrap
      raxmlBi.m = config("raxml_ml_model", default = "GTRGAMMAX")
      raxmlBi.p = config("seed", default = seed)
      raxmlBi.n = outputName + "_bi"
      raxmlBi.w = outputDir
      add(raxmlBi)
    }

    addRaxml(catVariantsSnps.output, outputDir + "raxml", "snps")
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
