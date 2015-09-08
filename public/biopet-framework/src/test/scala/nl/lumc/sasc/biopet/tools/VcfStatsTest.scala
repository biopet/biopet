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

import java.io.File
import java.nio.file.{Files, Paths}

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
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }


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
    val m1 : mutable.Map[Any, Int] = mutable.Map("a" -> 1)
    val m2 : mutable.Map[Any, Int] = mutable.Map("b" -> 2)

    mergeStatsMap(m1, m2)

    m1 should equal (mutable.Map("a" -> 1, "b" -> 2))

    val m3 : mutable.Map[Any, Int] = mutable.Map(1 -> 500)
    val m4 : mutable.Map[Any, Int] = mutable.Map(6 -> 125)

    mergeStatsMap(m3, m4)

    m3 should equal (mutable.Map(1 -> 500, 6 -> 125))

    mergeStatsMap(m1, m3)

    m1 should equal (mutable.Map("a" -> 1, "b" -> 2, 1 -> 500, 6 -> 125))
  }

  @Test
  def testMergeNestedStatsMap = {
    val m1 : mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]] = mutable.Map("test" ->
      mutable.Map("nested" -> mutable.Map("a" -> 1)))
    val m2: Map[String, Map[String, Map[Any, Int]]] = Map("test" ->
      Map("nested" -> Map("b" -> 2)))

    mergeNestedStatsMap(m1, m2)

    m1 should equal (mutable.Map("test" -> mutable.Map("nested" -> mutable.Map("a" -> 1, "b" -> 2))))

    val m3 : mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]] = mutable.Map("test" ->
      mutable.Map("nestedd" -> mutable.Map(1 -> 500)))
    val m4: Map[String, Map[String, Map[Any, Int]]] = Map("test" ->
      Map("nestedd" -> Map(6 -> 125)))

    mergeNestedStatsMap(m3, m4)

    m3 should equal (mutable.Map("test" -> mutable.Map("nestedd" -> mutable.Map(1 -> 500, 6 -> 125))))

    val m5 = m3.toMap.map(x => x._1 -> x._2.toMap.map(y => y._1 -> y._2.toMap))

    mergeNestedStatsMap(m1, m5)

    m1 should equal (mutable.Map("test" -> mutable.Map("nested" -> mutable.Map("a" -> 1, "b" -> 2),
    "nestedd" -> mutable.Map(1 -> 500, 6 -> 125))))
  }

  @Test
  def testValueOfTsv = {
    val i = new File(resourcePath("/sample.tsv"))

    valueFromTsv(i, "Sample_ID_1", "library") should be (Some("Lib_ID_1"))
    valueFromTsv(i, "Sample_ID_2", "library") should be (Some("Lib_ID_2"))
    valueFromTsv(i, "Sample_ID_1", "bam") should be (Some("MyFirst.bam"))
    valueFromTsv(i, "Sample_ID_2", "bam") should be (Some("MySecond.bam"))
    valueFromTsv(i, "Sample_ID_3", "bam") should be (empty)
  }

  @Test
  def testMain = {
    val tmp = Files.createTempDirectory("vcfStats")
    val vcf = resourcePath("/chrQ.vcf.gz")
    val ref = resourcePath("/fake_chrQ.fa")

    noException should be thrownBy main(Array("-I", vcf, "-R", ref, "-o", tmp.toAbsolutePath.toString))
    noException should be thrownBy main(Array("-I", vcf, "-R", ref, "-o", tmp.toAbsolutePath.toString, "--allInfoTags"))
    noException should be thrownBy main(Array("-I", vcf, "-R", ref, "-o",
      tmp.toAbsolutePath.toString, "--allInfoTags", "--allGenotypeTags"))
    noException should be thrownBy main(Array("-I", vcf, "-R", ref, "-o",
      tmp.toAbsolutePath.toString, "--binSize", "50", "--writeBinStats"))
    noException should be thrownBy main(Array("-I", vcf, "-R", ref, "-o",
      tmp.toAbsolutePath.toString, "--binSize", "50", "--writeBinStats",
      "--generalWiggle", "Total"))
    noException should be thrownBy main(Array("-I", vcf, "-R", ref, "-o",
      tmp.toAbsolutePath.toString, "--binSize", "50", "--writeBinStats",
      "--genotypeWiggle", "Total"))

    val genotypes = List("Het", "HetNonRef", "Hom", "HomRef", "HomVar", "Mixed", "NoCall", "NonInformative",
      "Available", "Called", "Filtered", "Variant")

    genotypes.foreach(
      x => noException should be thrownBy main(Array("-I", vcf, "-R", ref, "-o",
        tmp.toAbsolutePath.toString, "--binSize", "50", "--writeBinStats",
        "--genotypeWiggle", x))
    )

    val general = List("Biallelic", "ComplexIndel", "Filtered", "FullyDecoded", "Indel", "Mixed",
      "MNP", "MonomorphicInSamples", "NotFiltered", "PointEvent", "PolymorphicInSamples",
      "SimpleDeletion", "SimpleInsertion", "SNP", "StructuralIndel", "Symbolic",
      "SymbolicOrSV", "Variant")

    general.foreach(
      x => noException should be thrownBy main(Array("-I", vcf, "-R", ref, "-o",
        tmp.toAbsolutePath.toString, "--binSize", "50", "--writeBinStats",
        "--generalWiggle", x))
    )


    // returns null when validation fails
    def validateArgs(array: Array[String]): Option[Args] = {
      val argsParser = new OptParser
      argsParser.parse(array, Args())
    }

    validateArgs(Array("-I", vcf, "-R", ref, "-o",
      tmp.toAbsolutePath.toString, "--binSize", "50", "--writeBinStats",
      "--genotypeWiggle", "NonexistentThing")) shouldBe empty

    validateArgs(Array("-I", vcf, "-R", ref, "-o",
      tmp.toAbsolutePath.toString, "--binSize", "50", "--writeBinStats",
      "--generalWiggle", "NonexistentThing")) shouldBe empty

    validateArgs(Array("-R", ref, "-o",
      tmp.toAbsolutePath.toString)) shouldBe empty
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
