/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.basty

import java.io.File
import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.{ RunGubbins, Cat, Raxml }
import nl.lumc.sasc.biopet.pipelines.gatk.GatkPipeline
import nl.lumc.sasc.biopet.tools.BastyGenerateFasta
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.QScript

class Basty(val root: Configurable) extends QScript with MultiSampleQScript {
  qscript =>
  def this() = this(null)

  case class FastaOutput(variants: File, consensus: File, consensusVariants: File)

  override def defaults = ConfigUtils.mergeMaps(Map(
    "ploidy" -> 1,
    "use_haplotypecaller" -> false,
    "use_unifiedgenotyper" -> true,
    "joint_variantcalling" -> true
  ), super.defaults)

  var gatkPipeline: GatkPipeline = new GatkPipeline(qscript)

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    def makeLibrary(id: String) = new Library(id)
    class Library(libraryId: String) extends AbstractLibrary(libraryId) {
      protected def addJobs(): Unit = {}
    }

    var output: FastaOutput = _
    var outputSnps: FastaOutput = _

    protected def addJobs(): Unit = {
      addLibsJobs()
      output = addGenerateFasta(sampleId, sampleDir)
      outputSnps = addGenerateFasta(sampleId, sampleDir, snpsOnly = true)
    }
  }

  def init() {
    gatkPipeline.outputDir = outputDir
    gatkPipeline.init
  }

  def biopetScript() {
    gatkPipeline.biopetScript
    addAll(gatkPipeline.functions)

    val refVariants = addGenerateFasta(null, outputDir + "reference/", outputName = "reference")
    val refVariantSnps = addGenerateFasta(null, outputDir + "reference/", outputName = "reference", snpsOnly = true)

    addSamplesJobs()

    val catVariants = Cat(this, refVariants.variants :: samples.map(_._2.output.variants).toList, outputDir + "fastas/variant.fasta")
    add(catVariants)
    val catVariantsSnps = Cat(this, refVariantSnps.variants :: samples.map(_._2.outputSnps.variants).toList, outputDir + "fastas/variant.snps_only.fasta")
    add(catVariantsSnps)

    val catConsensus = Cat(this, refVariants.consensus :: samples.map(_._2.output.consensus).toList, outputDir + "fastas/consensus.fasta")
    add(catConsensus)
    val catConsensusSnps = Cat(this, refVariantSnps.consensus :: samples.map(_._2.outputSnps.consensus).toList, outputDir + "fastas/consensus.snps_only.fasta")
    add(catConsensusSnps)

    val catConsensusVariants = Cat(this, refVariants.consensusVariants :: samples.map(_._2.output.consensusVariants).toList, outputDir + "fastas/consensus.variant.fasta")
    add(catConsensusVariants)
    val catConsensusVariantsSnps = Cat(this, refVariantSnps.consensusVariants :: samples.map(_._2.outputSnps.consensusVariants).toList, outputDir + "fastas/consensus.variant.snps_only.fasta")
    add(catConsensusVariantsSnps)

    val seed: Int = config("seed", default = 12345)
    def addTreeJobs(variants: File, concensusVariants: File, outputDir: String, outputName: String) {
      val dirSufixRaxml = if (outputDir.endsWith(File.separator)) "raxml" else File.separator + "raxml"
      val dirSufixGubbins = if (outputDir.endsWith(File.separator)) "gubbins" else File.separator + "gubbins"

      val raxmlMl = new Raxml(this)
      raxmlMl.input = variants
      raxmlMl.m = config("raxml_ml_model", default = "GTRGAMMAX")
      raxmlMl.p = seed
      raxmlMl.n = outputName + "_ml"
      raxmlMl.w = outputDir + dirSufixRaxml
      raxmlMl.N = config("ml_runs", default = 20, submodule = "raxml")
      add(raxmlMl)

      val r = new scala.util.Random(seed)
      val numBoot = config("boot_runs", default = 100, submodule = "raxml").asInt
      val bootList = for (t <- 0 until numBoot) yield {
        val raxmlBoot = new Raxml(this)
        raxmlBoot.threads = 1
        raxmlBoot.input = variants
        raxmlBoot.m = config("raxml_ml_model", default = "GTRGAMMAX")
        raxmlBoot.p = seed
        raxmlBoot.b = math.abs(r.nextInt)
        raxmlBoot.w = outputDir + dirSufixRaxml
        raxmlBoot.N = 1
        raxmlBoot.n = outputName + "_boot_" + t
        add(raxmlBoot)
        raxmlBoot.getBootstrapFile
      }

      val cat = Cat(this, bootList.toList, outputDir + "/boot_list")
      add(cat)

      val raxmlBi = new Raxml(this)
      raxmlBi.input = concensusVariants
      raxmlBi.t = raxmlMl.getBestTreeFile
      raxmlBi.z = cat.output
      raxmlBi.m = config("raxml_ml_model", default = "GTRGAMMAX")
      raxmlBi.p = seed
      raxmlBi.f = "b"
      raxmlBi.n = outputName + "_bi"
      raxmlBi.w = outputDir + dirSufixRaxml
      add(raxmlBi)

      val gubbins = new RunGubbins(this)
      gubbins.fastafile = concensusVariants
      gubbins.startingTree = raxmlBi.getBipartitionsFile
      gubbins.outputDirectory = outputDir + dirSufixGubbins
      add(gubbins)
    }

    addTreeJobs(catVariantsSnps.output, catConsensusVariantsSnps.output, outputDir + "trees" + File.separator + "snps_only", "snps_only")
    addTreeJobs(catVariants.output, catConsensusVariants.output, outputDir + "trees" + File.separator + "snps_indels", "snps_indels")
  }

  def addGenerateFasta(sampleName: String, outputDir: String, outputName: String = null,
                       snpsOnly: Boolean = false): FastaOutput = {
    val bastyGenerateFasta = new BastyGenerateFasta(this)
    bastyGenerateFasta.outputName = if (outputName != null) outputName else sampleName
    bastyGenerateFasta.inputVcf = gatkPipeline.multisampleVariantcalling.scriptOutput.finalVcfFile
    if (gatkPipeline.samples.contains(sampleName)) {
      bastyGenerateFasta.bamFile = gatkPipeline.samples(sampleName).gatkVariantcalling.scriptOutput.bamFiles.head
    }
    bastyGenerateFasta.outputVariants = outputDir + bastyGenerateFasta.outputName + ".variants" + (if (snpsOnly) ".snps_only" else "") + ".fasta"
    bastyGenerateFasta.outputConsensus = outputDir + bastyGenerateFasta.outputName + ".consensus" + (if (snpsOnly) ".snps_only" else "") + ".fasta"
    bastyGenerateFasta.outputConsensusVariants = outputDir + bastyGenerateFasta.outputName + ".consensus_variants" + (if (snpsOnly) ".snps_only" else "") + ".fasta"
    bastyGenerateFasta.sampleName = sampleName
    bastyGenerateFasta.snpsOnly = snpsOnly
    qscript.add(bastyGenerateFasta)
    return FastaOutput(bastyGenerateFasta.outputVariants, bastyGenerateFasta.outputConsensus, bastyGenerateFasta.outputConsensusVariants)
  }
}

object Basty extends PipelineCommand
