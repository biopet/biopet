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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.bammetrics

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.scripts.CoverageStats
import org.broadinstitute.gatk.queue.QScript
import nl.lumc.sasc.biopet.core.{ SampleLibraryTag, BiopetQScript, PipelineCommand }
import java.io.File
import nl.lumc.sasc.biopet.tools.{ BedToInterval, BiopetFlagstat }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.bedtools.{ BedtoolsCoverage, BedtoolsIntersect }
import nl.lumc.sasc.biopet.extensions.picard.{ CollectInsertSizeMetrics, CollectGcBiasMetrics, CalculateHsMetrics, CollectAlignmentSummaryMetrics }
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsFlagstat

class BamMetrics(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {
  def this() = this(null)

  @Input(doc = "Bam File", shortName = "BAM", required = true)
  var inputBam: File = _

  @Input(doc = "Bed tracks targets", shortName = "target", required = false)
  var bedFiles: List[File] = Nil

  @Input(doc = "Bed tracks bait", shortName = "bait", required = false)
  var baitBedFile: File = _

  @Argument(doc = "", required = false)
  var wholeGenome = false

  def summaryFile = (sampleId, libId) match {
    case (Some(sampleId), Some(libId)) => new File(outputDir, sampleId + "-" + libId + ".BamMetrics.summary.json")
    case (Some(sampleId), _)           => new File(outputDir, sampleId + ".BamMetrics.summary.json")
    case _                             => new File(outputDir, "BamMetrics.summary.json")
  }

  def summaryFiles = Map("input_bam" -> inputBam)

  def summaryData = Map()

  def init() {
    if (config.contains("target_bed")) {
      for (file <- config("target_bed").asList) {
        bedFiles +:= new File(file.toString)
      }
    }
    if (baitBedFile == null && config.contains("target_bait")) baitBedFile = config("target_bait")
  }

  def biopetScript() {
    add(SamtoolsFlagstat(this, inputBam, swapExt(outputDir, inputBam, ".bam", ".flagstat")))
    add(BiopetFlagstat(this, inputBam, swapExt(outputDir, inputBam, ".bam", ".biopetflagstat")))
    add(CollectGcBiasMetrics(this, inputBam, outputDir))
    add(CollectInsertSizeMetrics(this, inputBam, outputDir))
    add(CollectAlignmentSummaryMetrics(this, inputBam, outputDir))

    val baitIntervalFile = if (baitBedFile != null) new File(outputDir, baitBedFile.getName.stripSuffix(".bed") + ".interval") else null
    if (baitIntervalFile != null)
      add(BedToInterval(this, baitBedFile, inputBam, outputDir), true)

    for (bedFile <- bedFiles) {
      val targetDir = new File(outputDir, bedFile.getName.stripSuffix(".bed"))
      val targetInterval = BedToInterval(this, bedFile, inputBam, targetDir)
      add(targetInterval, true)
      add(CalculateHsMetrics(this, inputBam, if (baitIntervalFile != null) baitIntervalFile
      else targetInterval.output, targetInterval.output, targetDir))

      val strictOutputBam = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".overlap.strict.bam")
      add(BedtoolsIntersect(this, inputBam, bedFile, strictOutputBam, minOverlap = config("strictintersectoverlap", default = 1.0)), true)
      add(SamtoolsFlagstat(this, strictOutputBam, swapExt(targetDir, strictOutputBam, ".bam", ".flagstat")))
      add(BiopetFlagstat(this, strictOutputBam, swapExt(targetDir, strictOutputBam, ".bam", ".biopetflagstat")))

      val looseOutputBam = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".overlap.loose.bam")
      add(BedtoolsIntersect(this, inputBam, bedFile, looseOutputBam, minOverlap = config("looseintersectoverlap", default = 0.01)), true)
      add(SamtoolsFlagstat(this, looseOutputBam, swapExt(targetDir, looseOutputBam, ".bam", ".biopet")))
      add(BiopetFlagstat(this, looseOutputBam, swapExt(targetDir, looseOutputBam, ".bam", ".biopetflagstat")))

      val coverageFile = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".coverage")
      add(BedtoolsCoverage(this, inputBam, bedFile, coverageFile, true), true)
      add(CoverageStats(this, coverageFile, targetDir))
    }

    addSummaryJobs
  }
}

object BamMetrics extends PipelineCommand {
  def apply(root: Configurable, bamFile: File, outputDir: File): BamMetrics = {
    val bamMetrics = new BamMetrics(root)
    bamMetrics.inputBam = bamFile
    bamMetrics.outputDir = outputDir

    bamMetrics.init
    bamMetrics.biopetScript
    return bamMetrics
  }
}
