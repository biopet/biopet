package nl.lumc.sasc.biopet.tools

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import scala.collection.mutable
import VcfStats._

/**
 * Created by pjvan_thof on 2/5/15.
 */
class VcfStatsTest extends TestNGSuite with Matchers {

  @Test
  def testSampleToSampleStats: Unit = {
    val s1 = SampleToSampleStats()
    val s2 = SampleToSampleStats()
    s1.alleleOverlap shouldBe 0
    s1.genotypeOverlap shouldBe 0
    s2.alleleOverlap shouldBe 0
    s2.genotypeOverlap shouldBe 0

    s1 += s2
    s1.alleleOverlap shouldBe 0
    s1.genotypeOverlap shouldBe 0
    s2.alleleOverlap shouldBe 0
    s2.genotypeOverlap shouldBe 0

    s2.alleleOverlap = 2
    s2.genotypeOverlap = 3

    s1 += s2
    s1.alleleOverlap shouldBe 2
    s1.genotypeOverlap shouldBe 3
    s2.alleleOverlap shouldBe 2
    s2.genotypeOverlap shouldBe 3

    s1 += s2
    s1.alleleOverlap shouldBe 4
    s1.genotypeOverlap shouldBe 6
    s2.alleleOverlap shouldBe 2
    s2.genotypeOverlap shouldBe 3
  }

  @Test
  def testSampleStats: Unit = {
    val s1 = SampleStats()
    val s2 = SampleStats()

    s1.sampleToSample += "s1" -> SampleToSampleStats()
    s1.sampleToSample += "s2" -> SampleToSampleStats()
    s2.sampleToSample += "s1" -> SampleToSampleStats()
    s2.sampleToSample += "s2" -> SampleToSampleStats()

    s1.sampleToSample("s1").alleleOverlap = 1
    s2.sampleToSample("s2").alleleOverlap = 2

    val bla1 = s1.genotypeStats.getOrElse("chr", mutable.Map[String, mutable.Map[Any, Int]]()) += "1" -> mutable.Map(1 -> 1)
    s1.genotypeStats += "chr" -> bla1
    val bla2 = s2.genotypeStats.getOrElse("chr", mutable.Map[String, mutable.Map[Any, Int]]()) += "2" -> mutable.Map(2 -> 2)
    s2.genotypeStats += "chr" -> bla2

    val ss1 = SampleToSampleStats()
    val ss2 = SampleToSampleStats()

    s1 += s2
    s1.genotypeStats.getOrElse("chr", mutable.Map[String, mutable.Map[Any, Int]]()) shouldBe mutable.Map("1" -> mutable.Map(1 -> 1), "2" -> mutable.Map(2 -> 2))
    ss1.alleleOverlap = 1
    ss2.alleleOverlap = 2
    s1.sampleToSample shouldBe mutable.Map("s1" -> ss1, "s2" -> ss2)

    s1 += s2
    s1.genotypeStats.getOrElse("chr", mutable.Map[String, mutable.Map[Any, Int]]()) shouldBe mutable.Map("1" -> mutable.Map(1 -> 1), "2" -> mutable.Map(2 -> 4))

    s1 += s1
    s1.genotypeStats.getOrElse("chr", mutable.Map[String, mutable.Map[Any, Int]]()) shouldBe mutable.Map("1" -> mutable.Map(1 -> 2), "2" -> mutable.Map(2 -> 8))
  }
}
