package nl.lumc.sasc.biopet.tools.bamstats

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 19-7-16.
 */
class StatsTest extends TestNGSuite with Matchers {
  @Test
  def testEqual(): Unit = {
    val s1 = new Stats()
    val s2 = new Stats()

    s1 shouldBe s2

    s1.mappingQualityHistogram.add(1)
    s1 should not be s2

    s2.mappingQualityHistogram.add(1)
    s1 shouldBe s2
  }

  @Test
  def testEmpty(): Unit = {
    val stats = new Stats()

    stats.clippingHistogram.countsMap shouldBe empty
    stats.insertSizeHistogram.countsMap shouldBe empty
    stats.mappingQualityHistogram.countsMap shouldBe empty
    stats.leftClippingHistogram.countsMap shouldBe empty
    stats.rightClippingHistogram.countsMap shouldBe empty
    stats._5_ClippingHistogram.countsMap shouldBe empty
    stats._3_ClippingHistogram.countsMap shouldBe empty
  }

  @Test
  def testPlus: Unit = {
    val s1 = new Stats()
    val s2 = new Stats()

    s2._3_ClippingHistogram.add(1)

    s1._3_ClippingHistogram.get(1) shouldBe None
    s1 += s2
    s1._3_ClippingHistogram.get(1) shouldBe Some(1)
  }
}
