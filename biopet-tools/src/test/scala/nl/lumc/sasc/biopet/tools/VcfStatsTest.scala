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
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.{Files, Paths}

import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.tools.vcfstats._
import nl.lumc.sasc.biopet.tools.vcfstats.VcfStats._
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import nl.lumc.sasc.biopet.utils.sortAnyAny
import org.apache.commons.io.FileUtils

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
    val s1 = SampleStats(sampleToSample = Array.fill(2)(SampleToSampleStats()))
    val s2 = SampleStats(sampleToSample = Array.fill(2)(SampleToSampleStats()))

    s1.sampleToSample(0).alleleOverlap = 1
    s2.sampleToSample(1).alleleOverlap = 2

    s1.genotypeStats += "1" -> mutable.Map(1 -> 1)
    s2.genotypeStats += "2" -> mutable.Map(2 -> 2)

    val ss1 = SampleToSampleStats()
    val ss2 = SampleToSampleStats()

    s1 += s2
    s1.genotypeStats shouldBe mutable.Map("1" -> mutable.Map(1 -> 1), "2" -> mutable.Map(2 -> 2))
    ss1.alleleOverlap = 1
    ss2.alleleOverlap = 2
    s1.sampleToSample shouldBe Array(ss1, ss2)

    s1 += s2
    s1.genotypeStats shouldBe mutable.Map("1" -> mutable.Map(1 -> 1), "2" -> mutable.Map(2 -> 4))

    s1 += s1
    s1.genotypeStats shouldBe mutable.Map("1" -> mutable.Map(1 -> 2), "2" -> mutable.Map(2 -> 8))
  }

  @Test
  def testMergeStatsMap(): Unit = {
    val m1: mutable.Map[Any, Int] = mutable.Map("a" -> 1)
    val m2: mutable.Map[Any, Int] = mutable.Map("b" -> 2)

    Stats.mergeStatsMap(m1, m2)

    m1 should equal(mutable.Map("a" -> 1, "b" -> 2))

    val m3: mutable.Map[Any, Int] = mutable.Map(1 -> 500)
    val m4: mutable.Map[Any, Int] = mutable.Map(6 -> 125)

    Stats.mergeStatsMap(m3, m4)

    m3 should equal(mutable.Map(1 -> 500, 6 -> 125))

    Stats.mergeStatsMap(m1, m3)

    m1 should equal(mutable.Map("a" -> 1, "b" -> 2, 1 -> 500, 6 -> 125))
  }

  @Test
  def testMergeNestedStatsMap(): Unit = {
    val m1: mutable.Map[String, mutable.Map[Any, Int]] =
      mutable.Map("nested" -> mutable.Map("a" -> 1))
    val m2: Map[String, Map[Any, Int]] = Map("nested" -> Map("b" -> 2))

    Stats.mergeNestedStatsMap(m1, m2)

    m1 should equal(mutable.Map("nested" -> mutable.Map("a" -> 1, "b" -> 2)))

    val m3: mutable.Map[String, mutable.Map[Any, Int]] =
      mutable.Map("nestedd" -> mutable.Map(1 -> 500))
    val m4: Map[String, Map[Any, Int]] = Map("nestedd" -> Map(6 -> 125))

    Stats.mergeNestedStatsMap(m3, m4)

    m3 should equal(mutable.Map("nestedd" -> mutable.Map(1 -> 500, 6 -> 125)))
  }

  @Test
  def testNoExistOutputDir(): Unit = {
    val tmp = Files.createTempDirectory("vcfStats")
    FileUtils.deleteDirectory(new File(tmp.toAbsolutePath.toString))
    val vcf = resourcePath("/chrQ.vcf.gz")
    val ref = resourcePath("/fake_chrQ.fa")

    an[IllegalArgumentException] should be thrownBy main(
      Array("-I", vcf, "-R", ref, "-o", tmp.toAbsolutePath.toString))
  }

  @Test
  def testMain(): Unit = {
    val tmp = Files.createTempDirectory("vcfStats")
    val vcf = resourcePath("/chrQ.vcf.gz")
    val ref = resourcePath("/fake_chrQ.fa")

    noException should be thrownBy main(
      Array("-I", vcf, "-R", ref, "-o", tmp.toAbsolutePath.toString))
    noException should be thrownBy main(
      Array("-I", vcf, "-R", ref, "-o", tmp.toAbsolutePath.toString, "--allInfoTags"))
    noException should be thrownBy main(
      Array("-I",
            vcf,
            "-R",
            ref,
            "-o",
            tmp.toAbsolutePath.toString,
            "--allInfoTags",
            "--allGenotypeTags"))
    noException should be thrownBy main(
      Array("-I",
            vcf,
            "-R",
            ref,
            "-o",
            tmp.toAbsolutePath.toString,
            "--binSize",
            "50",
            "--writeBinStats"))

    // returns null when validation fails
    def validateArgs(array: Array[String]): Option[VcfStatsArgs] = {
      val argsParser = new VcfStatsOptParser("vcfstats")
      argsParser.parse(array, VcfStatsArgs())
    }

    val stderr1 = new java.io.ByteArrayOutputStream
    Console.withErr(stderr1) {
      validateArgs(
        Array("-I",
              vcf,
              "-R",
              ref,
              "-o",
              tmp.toAbsolutePath.toString,
              "--binSize",
              "50",
              "--writeBinStats",
              "--genotypeWiggle",
              "NonexistentThing")) shouldBe empty
    }

    val stderr2 = new java.io.ByteArrayOutputStream
    Console.withErr(stderr2) {
      validateArgs(
        Array("-I",
              vcf,
              "-R",
              ref,
              "-o",
              tmp.toAbsolutePath.toString,
              "--binSize",
              "50",
              "--writeBinStats",
              "--generalWiggle",
              "NonexistentThing")) shouldBe empty
    }

    val stderr3 = new java.io.ByteArrayOutputStream
    Console.withErr(stderr3) {
      validateArgs(Array("-R", ref, "-o", tmp.toAbsolutePath.toString)) shouldBe empty
    }
  }

  @Test
  def testSortAnyAny(): Unit = {
    //stub
    val one: Any = 1
    val two: Any = 2
    val text: Any = "hello"
    val text2: Any = "goodbye"

    sortAnyAny(one, two) shouldBe true
    sortAnyAny(two, one) shouldBe false
    sortAnyAny(text, text2) shouldBe false
    sortAnyAny(text2, text) shouldBe true
    sortAnyAny(one, text) shouldBe true
    sortAnyAny(text, one) shouldBe false
  }

  @Test
  def testCheckGeneral(): Unit = {
    val record = new VCFFileReader(new File(resourcePath("/chrQ.vcf.gz"))).iterator().next()

    val generalStats = checkGeneral(record, List())

    generalStats.get("SampleDistribution-NonInformative") shouldEqual Some(Map(0 -> 1))
    generalStats.get("SampleDistribution-Called") shouldEqual Some(Map(3 -> 1))
    generalStats.get("SampleDistribution-Mixed") shouldEqual Some(Map(0 -> 1))
    generalStats.get("SampleDistribution-Hom") shouldEqual Some(Map(1 -> 1))
    generalStats.get("SampleDistribution-HomRef") shouldEqual Some(Map(1 -> 1))
    generalStats.get("SampleDistribution-Available") shouldEqual Some(Map(3 -> 1))
    generalStats.get("QUAL") shouldEqual Some(Map(1541 -> 1))
    generalStats.get("SampleDistribution-HetNonRef") shouldEqual Some(Map(0 -> 1))
    generalStats.get("SampleDistribution-Het") shouldEqual Some(Map(2 -> 1))
    generalStats.get("SampleDistribution-NoCall") shouldEqual Some(Map(0 -> 1))
    generalStats.get("SampleDistribution-Filtered") shouldEqual Some(Map(0 -> 1))
    generalStats.get("SampleDistribution-HomVar") shouldEqual Some(Map(0 -> 1))
    generalStats.get("SampleDistribution-Variant") shouldEqual Some(Map(2 -> 1))

    generalStats.get("general") should not be empty
    val general = generalStats("general")

    general.get("PolymorphicInSamples") shouldEqual Some(1)
    general.get("ComplexIndel") shouldEqual Some(0)
    general.get("FullyDecoded") shouldEqual Some(0)
    general.get("PointEvent") shouldEqual Some(0)
    general.get("MNP") shouldEqual Some(0)
    general.get("Indel") shouldEqual Some(1)
    general.get("Biallelic") shouldEqual Some(1)
    general.get("SimpleDeletion") shouldEqual Some(0)
    general.get("Variant") shouldEqual Some(1)
    general.get("SymbolicOrSV") shouldEqual Some(0)
    general.get("MonomorphicInSamples") shouldEqual Some(0)
    general.get("SNP") shouldEqual Some(0)
    general.get("Filtered") shouldEqual Some(0)
    general.get("StructuralIndel") shouldEqual Some(0)
    general.get("Total") shouldEqual Some(1)
    general.get("Mixed") shouldEqual Some(0)
    general.get("NotFiltered") shouldEqual Some(1)
    general.get("Symbolic") shouldEqual Some(0)
    general.get("SimpleInsertion") shouldEqual Some(1)
  }

  @Test
  def testCheckGenotype(): Unit = {
    val record = new VCFFileReader(new File(resourcePath("/chrQ.vcf.gz"))).iterator().next()

    val genotype = record.getGenotype(0)

    val genotypeStats = checkGenotype(record, genotype, List())

    genotypeStats.get("GQ") shouldEqual Some(Map(99 -> 1))
    genotypeStats.get("AD") shouldEqual Some(Map(24 -> 1, 21 -> 1))
    genotypeStats.get("AD-used") shouldEqual Some(Map(24 -> 1, 21 -> 1))
    genotypeStats.get("DP") shouldEqual Some(Map(45 -> 1))
    genotypeStats.get("AD-alt") shouldEqual Some(Map(21 -> 1))
    genotypeStats.get("AD-ref") shouldEqual Some(Map(24 -> 1))
    genotypeStats.get("general") should not be empty

    val general = genotypeStats("general")
    general.get("Hom") shouldEqual Some(0)
    general.get("NoCall") shouldEqual Some(0)
    general.get("Variant") shouldEqual Some(1)
    general.get("Filtered") shouldEqual Some(0)
    general.get("NonInformative") shouldEqual Some(0)
    general.get("Called") shouldEqual Some(1)
    general.get("Total") shouldEqual Some(1)
    general.get("HomVar") shouldEqual Some(0)
    general.get("HomRef") shouldEqual Some(0)
    general.get("Mixed") shouldEqual Some(0)
    general.get("Available") shouldEqual Some(1)
    general.get("Het") shouldEqual Some(1)
    general.get("HetNonRef") shouldEqual Some(0)
  }
}
