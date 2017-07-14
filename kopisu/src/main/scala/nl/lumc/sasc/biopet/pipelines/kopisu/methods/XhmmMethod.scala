/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.pipelines.kopisu.methods

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.extensions.gatk.DepthOfCoverage
import nl.lumc.sasc.biopet.extensions.tools.XcnvToBed
import nl.lumc.sasc.biopet.extensions.xhmm._
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
  * Created by Sander Bollen on 23-11-16.
  */
class XhmmMethod(val parent: Configurable) extends CnvMethod with Reference {

  def name = "xhmm"

  private var targets: File = config("amplicon_bed")
  val xhmmDir = new File(outputDir, "xhmm")

  override def fixedValues: Map[String, Any] = {
    super.fixedValues ++ Map(
      "depth_of_coverage" -> Map(
        "downsampling_type" -> "BY_SAMPLE",
        "downsample_to_coverage" -> 5000,
        "omit_depth_output_at_each_base" -> true,
        "omit_locus_table" -> true,
        "min_base_quality" -> 0,
        "min_mapping_quality" -> 20,
        "start" -> 1,
        "stop" -> 5000,
        "n_bins" -> 200,
        "include_ref_n_sites" -> true,
        "count_type" -> "COUNT_FRAGMENTS"
      )
    )
  }

  def biopetScript() = {
    val depths = inputBams.map { keyValuePair =>
      DepthOfCoverage(this,
                      List(keyValuePair._2),
                      swapExt(xhmmDir, keyValuePair._2, ".bam", ".dcov"),
                      List(targets))
    }.toList
    addAll(depths)

    // merging of gatk depths files
    val merged = new XhmmMergeGatkDepths(this)
    merged.gatkDepthsFiles = depths.map(_.intervalSummaryFile)
    merged.output = new File(xhmmDir, name + ".depths.data")
    add(merged)

    // the filtered and centered matrix
    val firstMatrix = new XhmmMatrix(this)
    firstMatrix.inputMatrix = merged.output
    firstMatrix.outputMatrix =
      swapExt(xhmmDir, merged.output, ".depths.data", ".filtered_centered.data")
    firstMatrix.outputExcludedSamples = Some(
      swapExt(xhmmDir, merged.output, ".depths.data", ".filtered.samples.txt"))
    firstMatrix.outputExcludedTargets = Some(
      swapExt(xhmmDir, merged.output, ".depths.data", ".filtered.targets.txt"))
    add(firstMatrix)

    // pca generation
    val pca = new XhmmPca(this)
    pca.inputMatrix = firstMatrix.outputMatrix
    pca.pcaFile =
      swapExt(xhmmDir, firstMatrix.outputMatrix, ".filtered_centered.data", ".rd_pca.data")
    add(pca)

    // normalization
    val normalize = new XhmmNormalize(this)
    normalize.inputMatrix = firstMatrix.outputMatrix
    normalize.pcaFile = pca.pcaFile
    normalize.normalizeOutput =
      swapExt(xhmmDir, firstMatrix.outputMatrix, ".filtered_centered.data", ".normalized.data")
    add(normalize)

    // normalized & filtered matrix
    val secondMatrix = new XhmmMatrix(this)
    secondMatrix.inputMatrix = normalize.normalizeOutput
    secondMatrix.centerData = true
    secondMatrix.centerType = "sample"
    secondMatrix.zScoreData = true
    secondMatrix.maxsdTargetRD = 30
    secondMatrix.outputExcludedTargets = Some(
      swapExt(xhmmDir, normalize.normalizeOutput, ".data", ".filtered.targets.txt"))
    secondMatrix.outputExcludedSamples = Some(
      swapExt(xhmmDir, normalize.normalizeOutput, ".data", ".filtered.samples.txt"))
    secondMatrix.outputMatrix =
      swapExt(xhmmDir, normalize.normalizeOutput, ".data", "filtered.data")
    add(secondMatrix)

    // re-synced matrix
    val thirdMatrix = new XhmmMatrix(this)
    thirdMatrix.inputMatrix = merged.output
    thirdMatrix.inputExcludeSamples =
      List(firstMatrix.outputExcludedSamples, secondMatrix.outputExcludedSamples).flatten
    thirdMatrix.inputExcludeTargets =
      List(firstMatrix.outputExcludedTargets, secondMatrix.outputExcludedTargets).flatten
    thirdMatrix.outputMatrix =
      swapExt(xhmmDir, merged.output, ".depths.data", ".same_filtered.data")
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

}
