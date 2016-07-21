package nl.lumc.sasc.biopet.tools.bamstats

/**
 * Created by pjvanthof on 05/07/16.
 */
case class Stats(var totalReads: Long = 0L,
                 var unmapped: Long = 0L,
                 var secondary: Long = 0L,
                 mappingQualityHistogram: Histogram[Int] = new Histogram[Int](),
                 insertSizeHistogram: Histogram[Int] = new Histogram[Int](),
                 clippingHistogram: Histogram[Int] = new Histogram[Int](),
                 leftClippingHistogram: Histogram[Int] = new Histogram[Int](),
                 rightClippingHistogram: Histogram[Int] = new Histogram[Int](),
                 _5_ClippingHistogram: Histogram[Int] = new Histogram[Int](),
                 _3_ClippingHistogram: Histogram[Int] = new Histogram[Int]()) {

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
