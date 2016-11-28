package nl.lumc.sasc.biopet.pipelines.kopisu.methods

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.extensions.gatk.DepthOfCoverage
import nl.lumc.sasc.biopet.extensions.tools.XcnvToBed
import nl.lumc.sasc.biopet.extensions.xhmm._
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by Sander Bollen on 23-11-16.
 */
class XhmmMethod(val root: Configurable) extends CnvMethod with Reference {

  def name = "xhmm"

  private var targets: Option[File] = None
  val xhmmDir = new File(outputDir, "xhmm")

  override def init() = {
    super.init()
    targets = config("amplicon_bed")
    if (targets.isEmpty) {
      throw new IllegalStateException("You must provide a BED file in key 'amplicon_bed' in your config to use XHMM")
    }
  }

  def biopetScript() = {
    val depths = inputBams.map(keyValuePair => depthOfCoverage(keyValuePair._2)).toList
    addAll(depths)

    // merging of gatk depths files
    val merged = new XhmmMergeGatkDepths(this)
    merged.gatkDepthsFiles = depths.map(_.intervalSummaryFile)
    merged.output = new File(xhmmDir, name + ".depths.data")
    add(merged)

    // the filtered and centered matrix
    val firstMatrix = new XhmmMatrix(this)
    firstMatrix.inputMatrix = merged.output
    firstMatrix.outputMatrix = swapExt(merged.output, ".depths.data", ".filtered_centered.data")
    firstMatrix.minTargetSize = 10
    firstMatrix.maxTargetSize = 10000
    firstMatrix.minMeanTargetRD = 10
    firstMatrix.maxMeanTargetRD = 500
    firstMatrix.minMeanSampleRD = 25
    firstMatrix.maxMeanSampleRD = 200
    firstMatrix.maxSdSampleRD = 150
    firstMatrix.outputExcludedSamples = Some(swapExt(merged.output, ".depths.data", ".filtered.samples.txt"))
    firstMatrix.outputExcludedTargets = Some(swapExt(merged.output, ".depths.data", ".filtered.targets.txt"))
    add(firstMatrix)

    // pca generation
    val pca = new XhmmPca(this)
    pca.inputMatrix = firstMatrix.outputMatrix
    pca.pcaFile = swapExt(firstMatrix.outputMatrix, ".filtered_centered.data", ".rd_pca.data")
    add(pca)

    // normalization
    val normalize = new XhmmNormalize(this)
    normalize.inputMatrix = firstMatrix.outputMatrix
    normalize.pcaFile = pca.pcaFile
    normalize.normalizeOutput = swapExt(firstMatrix.outputMatrix, ".filtered_centered.data", ".normalized.data")
    add(normalize)

    // normalized & filtered matrix
    val secondMatrix = new XhmmMatrix(this)
    secondMatrix.inputMatrix = normalize.normalizeOutput
    secondMatrix.centerData = true
    secondMatrix.centerType = "sample"
    secondMatrix.zScoreData = true
    secondMatrix.maxsdTargetRD = 30
    secondMatrix.outputExcludedTargets = Some(swapExt(normalize.normalizeOutput, ".data", ".filtered.targets.txt"))
    secondMatrix.outputExcludedSamples = Some(swapExt(normalize.normalizeOutput, ".data", ".filtered.samples.txt"))
    secondMatrix.outputMatrix = swapExt(normalize.normalizeOutput, ".data", "filtered.data")
    add(secondMatrix)

    // re-synced matrix
    val thirdMatrix = new XhmmMatrix(this)
    thirdMatrix.inputMatrix = merged.output
    thirdMatrix.inputExcludeSamples = List(firstMatrix.outputExcludedSamples, secondMatrix.outputExcludedSamples).flatten
    thirdMatrix.inputExcludeTargets = List(firstMatrix.outputExcludedTargets, secondMatrix.outputExcludedTargets).flatten
    thirdMatrix.outputMatrix = swapExt(merged.output, ".depths.data", ".same_filtered.data")
    add(thirdMatrix)

    // discovering cnvs
    val discover = new XhmmDiscover(this)
    discover.inputMatrix = secondMatrix.outputMatrix
    discover.r = thirdMatrix.outputMatrix
    discover.xhmmAnalysisName = name
    discover.outputXcnv = new File(xhmmDir, "xhmm.xcnv")
    add(discover)
    addSummarizable(discover, "xcnv")

    // generate vcf
    val genotype = new XhmmGenotype(this)
    genotype.inputXcnv = discover.outputXcnv
    genotype.inputMatrix = secondMatrix.outputMatrix
    genotype.r = thirdMatrix.outputMatrix
    genotype.outputVcf = new File(xhmmDir, "xhmm.vcf")
    add(genotype)

    // create bed files
    val bedDir = new File(xhmmDir, "beds")
    val beds = inputBams.keys.map { x =>
      val z = new XcnvToBed(this)
      z.inputXcnv = discover.outputXcnv
      z.sample = x
      z.outpuBed = new File(bedDir, s"$x.bed")
      z
    }
    addAll(beds)

    addSummaryJobs()

  }

  private def depthOfCoverage(bamFile: File): DepthOfCoverage = {
    val dp = new DepthOfCoverage(this)
    dp.input_file = List(bamFile)
    dp.intervals = targets.toList
    dp.downsampling_type = Some("BY_SAMPLE")
    dp.downsample_to_coverage = 5000
    dp.out = swapExt(xhmmDir, bamFile, ".bam", ".dcov")
    dp.omitDepthOutputAtEachBase = true
    dp.omitLocusTable = true
    dp.minBaseQuality = 0
    dp.minMappingQuality = 20
    dp.start = 1
    dp.stop = 5000
    dp.nBins = 200
    dp.includeRefNSites = true
    dp.countType = Some("COUNT_FRAGMENTS")
    dp
  }

}
