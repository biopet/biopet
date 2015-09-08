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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.tools

import htsjdk.variant.variantcontext.Allele
import nl.lumc.sasc.biopet.tools.VcfStats._
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.collection.mutable

/**
 * Test class for [[VcfStats]]
 *
 * Created by pjvan_thof on 2/5/15.
 */
class VcfStatsTest extends TestNGSuite with Matchers {

  @Test
  def testSampleToSampleStats(): Unit = {
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
  def testSampleStats(): Unit = {
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

  @Test
  def testAlleleOverlap(): Unit = {

    val a1 = Allele.create("G")
    val a2 = Allele.create("A")

    alleleOverlap(List(a1, a1), List(a1, a1)) shouldBe 2
    alleleOverlap(List(a2, a2), List(a2, a2)) shouldBe 2
    alleleOverlap(List(a1, a2), List(a1, a2)) shouldBe 2
    alleleOverlap(List(a1, a2), List(a2, a1)) shouldBe 2
    alleleOverlap(List(a2, a1), List(a1, a2)) shouldBe 2
    alleleOverlap(List(a2, a1), List(a2, a1)) shouldBe 2

    alleleOverlap(List(a1, a2), List(a1, a1)) shouldBe 1
    alleleOverlap(List(a2, a1), List(a1, a1)) shouldBe 1
    alleleOverlap(List(a1, a1), List(a1, a2)) shouldBe 1
    alleleOverlap(List(a1, a1), List(a2, a1)) shouldBe 1

    alleleOverlap(List(a1, a1), List(a2, a2)) shouldBe 0
    alleleOverlap(List(a2, a2), List(a1, a1)) shouldBe 0
  }

  @Test
  def testMergeStatsMap = {
    //stub
  }

  @Test
  def testMergeNestedStatsMap = {
    //stub
  }

  @Test
  def testValueOfTsv = {
    //stub
  }

  @Test
  def testMain = {
    //stub
  }

  @Test
  def testSortAnyAny = {
    //stub
  }

  @Test
  def testCheckGeneral = {
    //stub
  }

  @Test
  def testCheckGenotype = {
    //stub
  }
}
