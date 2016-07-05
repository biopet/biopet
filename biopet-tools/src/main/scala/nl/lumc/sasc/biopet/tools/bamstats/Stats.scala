package nl.lumc.sasc.biopet.tools.bamstats

import scala.collection.mutable

/**
 * Created by pjvanthof on 05/07/16.
 */
case class Stats() {

  var totalReads = 0L
  var unmapped = 0L
  var secondary = 0L
  var mappingQualityHistogram: mutable.Map[Int, Long] = mutable.Map()
  var insertSizeHistogram: mutable.Map[Int, Long] = mutable.Map()

  def +(other: Stats): Stats = {
    this.totalReads += other.totalReads
    this.unmapped += other.unmapped
    other.mappingQualityHistogram.foreach(x => this.mappingQualityHistogram += x._1 -> (this.mappingQualityHistogram.getOrElse(x._1, 0L) + x._2))
    other.insertSizeHistogram.foreach(x => this.insertSizeHistogram += x._1 -> (this.insertSizeHistogram.getOrElse(x._1, 0L) + x._2))
    this
  }
}
