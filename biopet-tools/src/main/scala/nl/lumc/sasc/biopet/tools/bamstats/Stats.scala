package nl.lumc.sasc.biopet.tools.bamstats

/**
 * Created by pjvanthof on 05/07/16.
 */
case class Stats() {

  var totalReads = 0L
  var unmapped = 0L
  var secondary = 0L
  val mappingQualityHistogram = Histogram()
  val insertSizeHistogram = Histogram()
  val clippingHistogram = Histogram()
  val leftClippingHistogram = Histogram()
  val rightClippingHistogram = Histogram()
  val _5_ClippingHistogram = Histogram()
  val _3_ClippingHistogram = Histogram()

  def +=(other: Stats): Stats = {
    this.totalReads += other.totalReads
    this.unmapped += other.unmapped
    this.mappingQualityHistogram += other.mappingQualityHistogram
    this.insertSizeHistogram += other.insertSizeHistogram
    this.clippingHistogram += other.clippingHistogram
    this.leftClippingHistogram += other.leftClippingHistogram
    this.rightClippingHistogram += other.rightClippingHistogram
    this._5_ClippingHistogram += other._5_ClippingHistogram
    this._3_ClippingHistogram += other._3_ClippingHistogram
    this
  }
}
