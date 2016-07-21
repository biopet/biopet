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
  flagstat.loadQualityFunctions(1, 0)
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
}
