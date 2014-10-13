package nl.lumc.sasc.biopet.pipelines.bammetrics

import nl.lumc.sasc.biopet.scripts.CoverageStats
import org.broadinstitute.gatk.queue.QScript
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import java.io.File
import nl.lumc.sasc.biopet.core.apps.{ BedToInterval, BiopetFlagstat }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.bedtools.{ BedtoolsCoverage, BedtoolsIntersect }
import nl.lumc.sasc.biopet.extensions.picard.{ CollectInsertSizeMetrics, CollectGcBiasMetrics, CalculateHsMetrics, CollectAlignmentSummaryMetrics }
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsFlagstat

class BamMetrics(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Bam File", shortName = "BAM", required = true)
  var inputBam: File = _

  @Input(doc = "Bed tracks targets", shortName = "target", required = false)
  var bedFiles: List[File] = Nil

  @Input(doc = "Bed tracks bait", shortName = "bait", required = false)
  var baitBedFile: File = _

  @Argument(doc = "", required = false)
  var wholeGenome = false

  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    if (outputDir == null) throw new IllegalStateException("Missing Output directory on BamMetrics module")
    else if (!outputDir.endsWith("/")) outputDir += "/"
    if (config.contains("target_bed")) {
      for (file <- config("target_bed").getList) {
        bedFiles +:= new File(file.toString)
      }
    }
    if (baitBedFile == null && config.contains("target_bait")) baitBedFile = config("target_bait")
  }

  def biopetScript() {
    add(SamtoolsFlagstat(this, inputBam, outputDir))
    add(BiopetFlagstat(this, inputBam, outputDir))
    add(CollectGcBiasMetrics(this, inputBam, outputDir))
    add(CollectInsertSizeMetrics(this, inputBam, outputDir))
    add(CollectAlignmentSummaryMetrics(this, inputBam, outputDir))

    val baitIntervalFile = if (baitBedFile != null) new File(outputDir, baitBedFile.getName.stripSuffix(".bed") + ".interval") else null
    if (baitIntervalFile != null)
      add(BedToInterval(this, baitBedFile, inputBam, outputDir), true)

    for (bedFile <- bedFiles) {
      val targetDir = outputDir + bedFile.getName.stripSuffix(".bed") + "/"
      val targetInterval = BedToInterval(this, bedFile, inputBam, targetDir)
      add(targetInterval, true)
      add(CalculateHsMetrics(this, inputBam, if (baitIntervalFile != null) baitIntervalFile
      else targetInterval.output, targetInterval.output, targetDir))

      val strictOutputBam = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".overlap.strict.bam")
      add(BedtoolsIntersect(this, inputBam, bedFile, strictOutputBam, minOverlap = config("strictintersectoverlap", default = 1.0)), true)
      add(SamtoolsFlagstat(this, strictOutputBam))
      add(BiopetFlagstat(this, strictOutputBam, targetDir))

      val looseOutputBam = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".overlap.loose.bam")
      add(BedtoolsIntersect(this, inputBam, bedFile, looseOutputBam, minOverlap = config("looseintersectoverlap", default = 0.01)), true)
      add(SamtoolsFlagstat(this, looseOutputBam))
      add(BiopetFlagstat(this, looseOutputBam, targetDir))

      val coverageFile = new File(targetDir, inputBam.getName.stripSuffix(".bam") + ".coverage")
      add(BedtoolsCoverage(this, inputBam, bedFile, coverageFile, true), true)
      add(CoverageStats(this, coverageFile, targetDir))
    }
  }
}

object BamMetrics extends PipelineCommand {
  def apply(root: Configurable, bamFile: File, outputDir: String): BamMetrics = {
    val bamMetrics = new BamMetrics(root)
    bamMetrics.inputBam = bamFile
    bamMetrics.outputDir = outputDir

    bamMetrics.init
    bamMetrics.biopetScript
    return bamMetrics
  }
}
