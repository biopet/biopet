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
import nl.lumc.sasc.biopet.extensions.picard._
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsFlagstat

class BamMetrics(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {
  def this() = this(null)

  @Input(doc = "Bam File", shortName = "BAM", required = true)
  var inputBam: File = _

  /** Bed files for region of interests */
  var roiBedFiles: List[File] = config("regions_of_interest", Nil)

  /** Bed of amplicon that is used */
  var ampliconBedFile: Option[File] = config("amplicon_bed")

  var rnaMetrics: Boolean = config("rna_metrcis", default = false)

  /** return location of summary file */
  def summaryFile = (sampleId, libId) match {
    case (Some(sampleId), Some(libId)) => new File(outputDir, sampleId + "-" + libId + ".BamMetrics.summary.json")
    case (Some(sampleId), _)           => new File(outputDir, sampleId + ".BamMetrics.summary.json")
    case _                             => new File(outputDir, "BamMetrics.summary.json")
  }

  /** returns files to store in summary */
  def summaryFiles = Map("input_bam" -> inputBam)

  /** return settings */
  def summarySettings = Map()

  /** executed before script */
  def init() {
  }

  /** Script to add jobs */
  def biopetScript() {
    add(SamtoolsFlagstat(this, inputBam, swapExt(outputDir, inputBam, ".bam", ".flagstat")))

    val biopetFlagstat = BiopetFlagstat(this, inputBam, outputDir)
    add(biopetFlagstat)
    addSummarizable(biopetFlagstat, "biopet_flagstat")

    val multiMetrics = new CollectMultipleMetrics(this)
    multiMetrics.input = inputBam
    multiMetrics.outputName = new File(outputDir, inputBam.getName.stripSuffix(".bam"))
    add(multiMetrics)

    add(CollectGcBiasMetrics(this, inputBam, outputDir))

    if (rnaMetrics) {
      val rnaMetrics = new CollectRnaSeqMetrics(this)
      rnaMetrics.input = inputBam
      rnaMetrics.output = swapExt(outputDir, inputBam, ".bam", ".rna.metrics")
      rnaMetrics.chartOutput = Some(swapExt(outputDir, inputBam, ".bam", ".rna.metrics.pdf"))
      add(rnaMetrics)
    }

    case class Intervals(bed: File, intervals: File)

    // Create temp jobs to convert bed files to intervals lists
    val roiIntervals = roiBedFiles.map(roiBedFile => {
      val roiIntervals = swapExt(outputDir, roiBedFile, ".bed", ".intervals")
      val ampBedToInterval = BedToIntervalList(this, roiBedFile, roiIntervals)
      ampBedToInterval.isIntermediate = true
      add(ampBedToInterval)
      Intervals(roiBedFile, roiIntervals)
    })

    // Metrics that require a amplicon bed file
    val ampIntervals = ampliconBedFile.collect {
      case ampliconBedFile => {
        val ampIntervals = swapExt(outputDir, ampliconBedFile, ".bed", ".intervals")
        val ampBedToInterval = BedToIntervalList(this, ampliconBedFile, ampIntervals)
        ampBedToInterval.isIntermediate = true
        add(ampBedToInterval)

        val chsMetrics = CalculateHsMetrics(this, inputBam,
          List(ampIntervals), ampIntervals :: roiIntervals.map(_.intervals), outputDir)
        add(chsMetrics)

        //TODO: target pcr metrics

        Intervals(ampliconBedFile, ampIntervals)
      }
    }

    // Create stats and coverage plot for each bed/interval file
    for (intervals <- roiIntervals ++ ampIntervals) {
      //TODO: Add target jobs to summary
      val targetDir = new File(outputDir, intervals.bed.getName.stripSuffix(".bed"))

      val biStrict = BedtoolsIntersect(this, inputBam, intervals.bed,
        output = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".overlap.strict.bam"),
        minOverlap = config("strictintersectoverlap", default = 1.0))
      biStrict.isIntermediate = true
      add(biStrict)
      add(SamtoolsFlagstat(this, biStrict.output, targetDir))
      add(BiopetFlagstat(this, biStrict.output, targetDir))

      val biLoose = BedtoolsIntersect(this, inputBam, intervals.bed,
        output = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".overlap.loose.bam"),
        minOverlap = config("looseintersectoverlap", default = 0.01))
      biLoose.isIntermediate = true
      add(biLoose)
      add(SamtoolsFlagstat(this, biLoose.output, targetDir))
      add(BiopetFlagstat(this, biLoose.output, targetDir))

      val coverageFile = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".coverage")

      //FIXME:should use piping
      add(BedtoolsCoverage(this, inputBam, intervals.bed, coverageFile, true), true)
      add(CoverageStats(this, coverageFile, targetDir))
    }

    addSummaryJobs
  }
}

object BamMetrics extends PipelineCommand {
  /** Make default implementation of BamMetrics and runs script already */
  def apply(root: Configurable, bamFile: File, outputDir: File): BamMetrics = {
    val bamMetrics = new BamMetrics(root)
    bamMetrics.inputBam = bamFile
    bamMetrics.outputDir = outputDir

    bamMetrics.init
    bamMetrics.biopetScript
    return bamMetrics
  }
}
