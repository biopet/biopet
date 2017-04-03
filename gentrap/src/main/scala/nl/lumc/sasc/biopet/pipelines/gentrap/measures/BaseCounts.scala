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
package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.core.annotations.AnnotationRefFlat
import nl.lumc.sasc.biopet.extensions.tools.BaseCounter
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
class BaseCounts(val parent: Configurable) extends QScript with Measurement with AnnotationRefFlat {

  def mergeArgs = MergeArgs(List(1), 2, numHeaderLines = 0, fallback = "0")

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val jobs = bamFiles.map {
      case (id, file) =>
        val baseCounter = new BaseCounter(this)
        baseCounter.bamFile = file
        baseCounter.outputDir = new File(outputDir, id)
        baseCounter.prefix = id
        baseCounter.refFlat = annotationRefFlat()
        add(baseCounter)
        id -> baseCounter
    }

    def addTableAndHeatmap(countFiles: List[File], outputName: String): Unit = {
      val mergedTable = new File(outputDir, s"$name.$outputName.tsv")
      val heatmapFile = new File(outputDir, s"$name.$outputName.png")
      addMergeTableJob(countFiles, mergedTable, outputName, countFiles.head.getName.stripPrefix(jobs.head._1))
      addHeatmapJob(mergedTable, heatmapFile, outputName)
    }

    addTableAndHeatmap(jobs.values.map(_.transcriptTotalCounts).toList, "transcriptTotalCounts")
    addTableAndHeatmap(jobs.values.map(_.transcriptTotalSenseCounts).toList, "transcriptTotalSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.transcriptTotalAntiSenseCounts).toList, "transcriptTotalAntiSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.transcriptExonicCounts).toList, "transcriptExonicCounts")
    addTableAndHeatmap(jobs.values.map(_.transcriptExonicSenseCounts).toList, "transcriptExonicSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.transcriptExonicAntiSenseCounts).toList, "transcriptExonicAntiSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.transcriptIntronicCounts).toList, "transcriptIntronicCounts")
    addTableAndHeatmap(jobs.values.map(_.transcriptIntronicSenseCounts).toList, "transcriptIntronicSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.transcriptIntronicAntiSenseCounts).toList, "transcriptIntronicAntiSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.exonCounts).toList, "exonCounts")
    addTableAndHeatmap(jobs.values.map(_.exonSenseCounts).toList, "exonSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.exonAntiSenseCounts).toList, "exonAntiSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.intronCounts).toList, "intronCounts")
    addTableAndHeatmap(jobs.values.map(_.intronSenseCounts).toList, "intronSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.intronAntiSenseCounts).toList, "intronAntiSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.geneTotalCounts).toList, "geneTotalCounts")
    addTableAndHeatmap(jobs.values.map(_.geneTotalSenseCounts).toList, "geneTotalSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.geneTotalAntiSenseCounts).toList, "geneTotalAntiSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.geneExonicCounts).toList, "geneExonicCounts")
    addTableAndHeatmap(jobs.values.map(_.geneExonicSenseCounts).toList, "geneExonicSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.geneExonicAntiSenseCounts).toList, "geneExonicAntiSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.geneIntronicCounts).toList, "geneIntronicCounts")
    addTableAndHeatmap(jobs.values.map(_.geneIntronicSenseCounts).toList, "geneIntronicSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.geneIntronicAntiSenseCounts).toList, "geneIntronicAntiSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.mergeExonCounts).toList, "mergeExonCounts")
    addTableAndHeatmap(jobs.values.map(_.mergeExonSenseCounts).toList, "mergeExonSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.mergeExonAntiSenseCounts).toList, "mergeExonAntiSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.mergeIntronCounts).toList, "mergeIntronCounts")
    addTableAndHeatmap(jobs.values.map(_.mergeIntronSenseCounts).toList, "mergeIntronSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.mergeIntronAntiSenseCounts).toList, "mergeIntronAntiSenseCounts")
    addTableAndHeatmap(jobs.values.map(_.nonStrandedMetaExonCounts).toList, "nonStrandedMetaExonCounts")
    addTableAndHeatmap(jobs.values.map(_.strandedMetaExonCounts).toList, "strandedMetaExonCounts")
    addTableAndHeatmap(jobs.values.map(_.strandedSenseMetaExonCounts).toList, "strandedSenseMetaExonCounts")
    addTableAndHeatmap(jobs.values.map(_.strandedAntiSenseMetaExonCounts).toList, "strandedAntiSenseMetaExonCounts")

    addSummaryJobs()
  }
}
