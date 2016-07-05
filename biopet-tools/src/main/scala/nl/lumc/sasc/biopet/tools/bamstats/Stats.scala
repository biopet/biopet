package nl.lumc.sasc.biopet.tools.bamstats

import scala.collection.mutable
import scala.concurrent.blocking


/**
 * Created by pjvanthof on 05/07/16.
 */
case class Stats() {

  var totalReads = 0L
  var unmapped = 0L
  var secondary = 0L
  val mappingQualityHistogram = Histogram()
  val insertSizeHistogram = Histogram()

  def +(other: Stats): Stats = {
    this.totalReads += other.totalReads
    this.unmapped += other.unmapped
    this.mappingQualityHistogram += other.mappingQualityHistogram
    this.insertSizeHistogram += other.insertSizeHistogram
    this
  }
}
