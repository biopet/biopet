package nl.lumc.sasc.biopet.tools.bamstats

/**
 * Created by pjvanthof on 05/07/16.
 */
case class Stats() {

  var totalReads = 0L
  var unmapped = 0L
  var secondary = 0L
  val mappingQualityHistogram = new Histogram[Int]()
  val insertSizeHistogram = new Histogram[Int]()
  val clippingHistogram = new Histogram[Int]()
  val leftClippingHistogram = new Histogram[Int]()
  val rightClippingHistogram = new Histogram[Int]()
  val _5_ClippingHistogram = new Histogram[Int]()
  val _3_ClippingHistogram = new Histogram[Int]()

  /** This will add an other [[Stats]] inside `this` */
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
