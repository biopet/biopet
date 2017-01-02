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
package nl.lumc.sasc.biopet.tools.bamstats

import java.io.File

import nl.lumc.sasc.biopet.tools.flagstat.FlagstatCollector

/**
 * Created by pjvanthof on 05/07/16.
 */
case class Stats(flagstat: FlagstatCollector = new FlagstatCollector(),
                 mappingQualityHistogram: Histogram[Int] = new Histogram[Int](),
                 insertSizeHistogram: Histogram[Int] = new Histogram[Int](),
                 clippingHistogram: Histogram[Int] = new Histogram[Int](),
                 leftClippingHistogram: Histogram[Int] = new Histogram[Int](),
                 rightClippingHistogram: Histogram[Int] = new Histogram[Int](),
                 _5_ClippingHistogram: Histogram[Int] = new Histogram[Int](),
                 _3_ClippingHistogram: Histogram[Int] = new Histogram[Int]()) {

  flagstat.loadDefaultFunctions()
  flagstat.loadQualityFunctions()
  flagstat.loadOrientationFunctions

  /** This will add an other [[Stats]] inside `this` */
  def +=(other: Stats): Stats = {
    this.flagstat += other.flagstat
    this.mappingQualityHistogram += other.mappingQualityHistogram
    this.insertSizeHistogram += other.insertSizeHistogram
    this.clippingHistogram += other.clippingHistogram
    this.leftClippingHistogram += other.leftClippingHistogram
    this.rightClippingHistogram += other.rightClippingHistogram
    this._5_ClippingHistogram += other._5_ClippingHistogram
    this._3_ClippingHistogram += other._3_ClippingHistogram
    this
  }

  def writeStatsToFiles(outputDir: File): Unit = {
    this.flagstat.writeReportToFile(new File(outputDir, "flagstats"))
    this.flagstat.writeSummaryTofile(new File(outputDir, "flagstats.summary.json"))
    this.mappingQualityHistogram.writeToTsv(new File(outputDir, "mapping_quality.tsv"))
    this.insertSizeHistogram.writeToTsv(new File(outputDir, "insert_size.tsv"))
    this.clippingHistogram.writeToTsv(new File(outputDir, "clipping.tsv"))
    this.leftClippingHistogram.writeToTsv(new File(outputDir, "left_clipping.tsv"))
    this.rightClippingHistogram.writeToTsv(new File(outputDir, "right_clipping.tsv"))
    this._5_ClippingHistogram.writeToTsv(new File(outputDir, "5_prime_clipping.tsv"))
    this._3_ClippingHistogram.writeToTsv(new File(outputDir, "3_prime_clipping.tsv"))
  }

  def toSummaryMap = {
    Map(
      "flagstats" -> flagstat.toSummaryMap,
      "mapping_quality" -> Map("histrogram" -> mappingQualityHistogram.toSummaryMap, "general" -> mappingQualityHistogram.aggregateStats),
      "insert_size" -> Map("histrogram" -> insertSizeHistogram.toSummaryMap, "general" -> insertSizeHistogram.aggregateStats),
      "clipping" -> Map("histrogram" -> clippingHistogram.toSummaryMap, "general" -> clippingHistogram.aggregateStats),
      "left_clipping" -> Map("histrogram" -> leftClippingHistogram.toSummaryMap, "general" -> leftClippingHistogram.aggregateStats),
      "right_clipping" -> Map("histrogram" -> rightClippingHistogram.toSummaryMap, "general" -> rightClippingHistogram.aggregateStats),
      "5_prime_clipping" -> Map("histrogram" -> _5_ClippingHistogram.toSummaryMap, "general" -> _5_ClippingHistogram.aggregateStats),
      "3_prime_clipping" -> Map("histrogram" -> _3_ClippingHistogram.toSummaryMap, "general" -> _3_ClippingHistogram.aggregateStats)
    )
  }
}
