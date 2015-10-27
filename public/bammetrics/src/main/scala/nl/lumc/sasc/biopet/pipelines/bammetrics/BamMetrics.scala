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

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{ BiopetFifoPipe, PipelineCommand, SampleLibraryTag }
import nl.lumc.sasc.biopet.extensions.bedtools.{ BedtoolsCoverage, BedtoolsIntersect }
import nl.lumc.sasc.biopet.extensions.picard._
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsFlagstat
import nl.lumc.sasc.biopet.pipelines.bammetrics.scripts.CoverageStats
import nl.lumc.sasc.biopet.extensions.tools.BiopetFlagstat
import org.broadinstitute.gatk.queue.QScript

class BamMetrics(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {
  def this() = this(null)

  @Input(doc = "Bam File", shortName = "BAM", required = true)
  var inputBam: File = _

  /** Bed files for region of interests */
  var roiBedFiles: List[File] = config("regions_of_interest", Nil)

  /** Bed of amplicon that is used */
  var ampliconBedFile: Option[File] = config("amplicon_bed")

  /** Settings for CollectRnaSeqMetrics */
  var rnaMetricsSettings: Map[String, String] = Map()
  var transcriptRefFlatFile: Option[File] = config("transcript_refflat")

  /** return location of summary file */
  def summaryFile = (sampleId, libId) match {
    case (Some(s), Some(l)) => new File(outputDir, s + "-" + l + ".BamMetrics.summary.json")
    case (Some(s), _)       => new File(outputDir, s + ".BamMetrics.summary.json")
    case _                  => new File(outputDir, "BamMetrics.summary.json")
  }

  /** returns files to store in summary */
  def summaryFiles = Map("input_bam" -> inputBam) ++
    ampliconBedFile.map("amplicon" -> _).toMap ++
    ampliconBedFile.map(x => "roi_" + x.getName.stripSuffix(".bed") -> x).toMap

  /** return settings */
  def summarySettings = Map("amplicon_name" -> ampliconBedFile.collect { case x => x.getName.stripSuffix(".bed") },
    "roi_name" -> roiBedFiles.map(_.getName.stripSuffix(".bed")))

  override def reportClass = {
    val bammetricsReport = new BammetricsReport(this)
    bammetricsReport.outputDir = new File(outputDir, "report")
    bammetricsReport.summaryFile = summaryFile
    bammetricsReport.args = if (libId.isDefined) Map(
      "sampleId" -> sampleId.getOrElse("."),
      "libId" -> libId.getOrElse("."))
    else Map("sampleId" -> sampleId.getOrElse("."))
    Some(bammetricsReport)
  }

  /** executed before script */
  def init(): Unit = {
    inputFiles :+= new InputFile(inputBam)
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
    addSummarizable(multiMetrics, "multi_metrics")

    val gcBiasMetrics = CollectGcBiasMetrics(this, inputBam, outputDir)
    add(gcBiasMetrics)
    addSummarizable(gcBiasMetrics, "gc_bias")

    if (transcriptRefFlatFile.isEmpty) {
      val wgsMetrics = new CollectWgsMetrics(this)
      wgsMetrics.input = inputBam
      wgsMetrics.output = swapExt(outputDir, inputBam, ".bam", ".wgs.metrics")
      add(wgsMetrics)
      addSummarizable(wgsMetrics, "wgs")
    }

    if (transcriptRefFlatFile.isDefined) {
      val rnaMetrics = new CollectRnaSeqMetrics(this)
      rnaMetrics.input = inputBam
      rnaMetrics.output = swapExt(outputDir, inputBam, ".bam", ".rna.metrics")
      rnaMetrics.chartOutput = Some(swapExt(outputDir, inputBam, ".bam", ".rna.metrics.pdf"))
      rnaMetrics.refFlat = transcriptRefFlatFile.get
      rnaMetrics.ribosomalIntervals = rnaMetricsSettings.get("ribosomal_intervals").collect { case n => new File(n) }
      rnaMetrics.strandSpecificity = rnaMetricsSettings.get("strand_specificity")
      add(rnaMetrics)
      addSummarizable(rnaMetrics, "rna")
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
      case bedFile =>
        val ampIntervals = swapExt(outputDir, bedFile, ".bed", ".intervals")
        val ampBedToInterval = BedToIntervalList(this, bedFile, ampIntervals)
        ampBedToInterval.isIntermediate = true
        add(ampBedToInterval)

        val chsMetrics = CalculateHsMetrics(this, inputBam,
          List(ampIntervals), ampIntervals :: roiIntervals.map(_.intervals), outputDir)
        add(chsMetrics)
        addSummarizable(chsMetrics, "hs_metrics")

        val pcrMetrics = CollectTargetedPcrMetrics(this, inputBam,
          ampIntervals, ampIntervals :: roiIntervals.map(_.intervals), outputDir)
        add(pcrMetrics)
        addSummarizable(chsMetrics, "targeted_pcr_metrics")

        Intervals(bedFile, ampIntervals)
    }

    // Create stats and coverage plot for each bed/interval file
    for (intervals <- roiIntervals ++ ampIntervals) {
      val targetName = intervals.bed.getName.stripSuffix(".bed")
      val targetDir = new File(outputDir, targetName)

      val biStrict = BedtoolsIntersect(this, inputBam, intervals.bed,
        output = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".overlap.strict.sam"),
        minOverlap = config("strict_intersect_overlap", default = 1.0))
      val biopetFlagstatStrict = BiopetFlagstat(this, biStrict.output, targetDir)
      addSummarizable(biopetFlagstatStrict, targetName + "_biopet_flagstat_strict")
      add(new BiopetFifoPipe(this, List(biStrict, biopetFlagstatStrict)))

      val biLoose = BedtoolsIntersect(this, inputBam, intervals.bed,
        output = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".overlap.loose.sam"),
        minOverlap = config("loose_intersect_overlap", default = 0.01))
      val biopetFlagstatLoose = BiopetFlagstat(this, biLoose.output, targetDir)
      addSummarizable(biopetFlagstatLoose, targetName + "_biopet_flagstat_loose")
      add(new BiopetFifoPipe(this, List(biLoose, biopetFlagstatLoose)))

      val bedCov = BedtoolsCoverage(this, intervals.bed, inputBam, depth = true)
      val covStats = CoverageStats(this, targetDir, inputBam.getName.stripSuffix(".bam") + ".coverage")
      covStats.title = Some("Coverage for " + targetName)
      covStats.subTitle = Some(".")
      add(bedCov | covStats)
      addSummarizable(covStats, targetName + "_cov_stats")
    }

    addSummaryJobs()
  }
}

object BamMetrics extends PipelineCommand {
  /** Make default implementation of BamMetrics and runs script already */
  def apply(root: Configurable,
            bamFile: File, outputDir: File,
            sampleId: Option[String] = None,
            libId: Option[String] = None): BamMetrics = {
    val bamMetrics = new BamMetrics(root)
    bamMetrics.sampleId = sampleId
    bamMetrics.libId = libId
    bamMetrics.inputBam = bamFile
    bamMetrics.outputDir = outputDir

    bamMetrics.init()
    bamMetrics.biopetScript()
    bamMetrics
  }
}
