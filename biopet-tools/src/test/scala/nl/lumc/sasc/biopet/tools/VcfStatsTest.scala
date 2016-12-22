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

import htsjdk.variant.variantcontext.Allele
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.tools.vcfstats.{SampleStats, SampleToSampleStats, Stats, VcfStats}
import nl.lumc.sasc.biopet.tools.vcfstats.VcfStats._
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import nl.lumc.sasc.biopet.utils.sortAnyAny

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
  def testMergeStatsMap = {
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
  def testMergeNestedStatsMap = {
    val m1: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]] = mutable.Map("test" ->
      mutable.Map("nested" -> mutable.Map("a" -> 1)))
    val m2: Map[String, Map[String, Map[Any, Int]]] = Map("test" ->
      Map("nested" -> Map("b" -> 2)))

    Stats.mergeNestedStatsMap(m1, m2)

    m1 should equal(mutable.Map("test" -> mutable.Map("nested" -> mutable.Map("a" -> 1, "b" -> 2))))

    val m3: mutable.Map[String, mutable.Map[String, mutable.Map[Any, Int]]] = mutable.Map("test" ->
      mutable.Map("nestedd" -> mutable.Map(1 -> 500)))
    val m4: Map[String, Map[String, Map[Any, Int]]] = Map("test" ->
      Map("nestedd" -> Map(6 -> 125)))

    Stats.mergeNestedStatsMap(m3, m4)

    m3 should equal(mutable.Map("test" -> mutable.Map("nestedd" -> mutable.Map(1 -> 500, 6 -> 125))))

    val m5 = m3.toMap.map(x => x._1 -> x._2.toMap.map(y => y._1 -> y._2.toMap))

    Stats.mergeNestedStatsMap(m1, m5)

    m1 should equal(mutable.Map("test" -> mutable.Map("nested" -> mutable.Map("a" -> 1, "b" -> 2),
      "nestedd" -> mutable.Map(1 -> 500, 6 -> 125))))
  }

  @Test
  def testValueOfTsv = {
    val i = new File(resourcePath("/sample.tsv"))

    valueFromTsv(i, "Sample_ID_1", "library") should be(Some("Lib_ID_1"))
    valueFromTsv(i, "Sample_ID_2", "library") should be(Some("Lib_ID_2"))
    valueFromTsv(i, "Sample_ID_1", "bam") should be(Some("MyFirst.bam"))
    valueFromTsv(i, "Sample_ID_2", "bam") should be(Some("MySecond.bam"))
    valueFromTsv(i, "Sample_ID_3", "bam") should be(empty)
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

    val stderr1 = new java.io.ByteArrayOutputStream
    Console.withErr(stderr1) {
      validateArgs(Array("-I", vcf, "-R", ref, "-o",
        tmp.toAbsolutePath.toString, "--binSize", "50", "--writeBinStats",
        "--genotypeWiggle", "NonexistentThing")) shouldBe empty
    }

    val stderr2 = new java.io.ByteArrayOutputStream
    Console.withErr(stderr2) {
      validateArgs(Array("-I", vcf, "-R", ref, "-o",
        tmp.toAbsolutePath.toString, "--binSize", "50", "--writeBinStats",
        "--generalWiggle", "NonexistentThing")) shouldBe empty
    }

    val stderr3 = new java.io.ByteArrayOutputStream
    Console.withErr(stderr3) {
      validateArgs(Array("-R", ref, "-o",
        tmp.toAbsolutePath.toString)) shouldBe empty
    }
  }

  @Test
  def testSortAnyAny = {
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
  def testCheckGeneral = {
    val record = new VCFFileReader(new File(resourcePath("/chrQ.vcf.gz"))).iterator().next()

    val blah = checkGeneral(record, List())

    blah.get("chrQ") should not be empty
    blah.get("total") should not be empty

    val chrq = blah.get("chrQ").get
    chrq.get("SampleDistribution-NonInformative") shouldEqual Some(Map(0 -> 1))
    chrq.get("SampleDistribution-Called") shouldEqual Some(Map(3 -> 1))
    chrq.get("SampleDistribution-Mixed") shouldEqual Some(Map(0 -> 1))
    chrq.get("SampleDistribution-Hom") shouldEqual Some(Map(1 -> 1))
    chrq.get("SampleDistribution-HomRef") shouldEqual Some(Map(1 -> 1))
    chrq.get("SampleDistribution-Available") shouldEqual Some(Map(3 -> 1))
    chrq.get("QUAL") shouldEqual Some(Map(1541 -> 1))
    chrq.get("SampleDistribution-HetNonRef") shouldEqual Some(Map(0 -> 1))
    chrq.get("SampleDistribution-Het") shouldEqual Some(Map(2 -> 1))
    chrq.get("SampleDistribution-NoCall") shouldEqual Some(Map(0 -> 1))
    chrq.get("SampleDistribution-Filtered") shouldEqual Some(Map(0 -> 1))
    chrq.get("SampleDistribution-HomVar") shouldEqual Some(Map(0 -> 1))
    chrq.get("SampleDistribution-Variant") shouldEqual Some(Map(2 -> 1))

    chrq.get("general") should not be empty
    val general = chrq.get("general").get

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

    val total = blah.get("total").get
    total.get("SampleDistribution-NonInformative") shouldEqual Some(Map(0 -> 1))
    total.get("SampleDistribution-Called") shouldEqual Some(Map(3 -> 1))
    total.get("SampleDistribution-Mixed") shouldEqual Some(Map(0 -> 1))
    total.get("SampleDistribution-Hom") shouldEqual Some(Map(1 -> 1))
    total.get("SampleDistribution-HomRef") shouldEqual Some(Map(1 -> 1))
    total.get("SampleDistribution-Available") shouldEqual Some(Map(3 -> 1))
    total.get("QUAL") shouldEqual Some(Map(1541 -> 1))
    total.get("SampleDistribution-HetNonRef") shouldEqual Some(Map(0 -> 1))
    total.get("SampleDistribution-Het") shouldEqual Some(Map(2 -> 1))
    total.get("SampleDistribution-NoCall") shouldEqual Some(Map(0 -> 1))
    total.get("SampleDistribution-Filtered") shouldEqual Some(Map(0 -> 1))
    total.get("SampleDistribution-HomVar") shouldEqual Some(Map(0 -> 1))
    total.get("SampleDistribution-Variant") shouldEqual Some(Map(2 -> 1))

    chrq.get("general") should not be empty
    val totGeneral = total.get("general").get

    totGeneral.get("PolymorphicInSamples") shouldEqual Some(1)
    totGeneral.get("ComplexIndel") shouldEqual Some(0)
    totGeneral.get("FullyDecoded") shouldEqual Some(0)
    totGeneral.get("PointEvent") shouldEqual Some(0)
    totGeneral.get("MNP") shouldEqual Some(0)
    totGeneral.get("Indel") shouldEqual Some(1)
    totGeneral.get("Biallelic") shouldEqual Some(1)
    totGeneral.get("SimpleDeletion") shouldEqual Some(0)
    totGeneral.get("Variant") shouldEqual Some(1)
    totGeneral.get("SymbolicOrSV") shouldEqual Some(0)
    totGeneral.get("MonomorphicInSamples") shouldEqual Some(0)
    totGeneral.get("SNP") shouldEqual Some(0)
    totGeneral.get("Filtered") shouldEqual Some(0)
    totGeneral.get("StructuralIndel") shouldEqual Some(0)
    totGeneral.get("Total") shouldEqual Some(1)
    totGeneral.get("Mixed") shouldEqual Some(0)
    totGeneral.get("NotFiltered") shouldEqual Some(1)
    totGeneral.get("Symbolic") shouldEqual Some(0)
    totGeneral.get("SimpleInsertion") shouldEqual Some(1)
  }

  @Test
  def testCheckGenotype = {
    val record = new VCFFileReader(new File(resourcePath("/chrQ.vcf.gz"))).iterator().next()

    val genotype = record.getGenotype(0)

    val blah = checkGenotype(record, genotype, List())

    blah.get("chrQ") should not be empty
    blah.get("total") should not be empty

    val chrq = blah.get("chrQ").get
    chrq.get("GQ") shouldEqual Some(Map(99 -> 1))
    chrq.get("AD") shouldEqual Some(Map(24 -> 1, 21 -> 1))
    chrq.get("AD-used") shouldEqual Some(Map(24 -> 1, 21 -> 1))
    chrq.get("DP") shouldEqual Some(Map(45 -> 1))
    chrq.get("AD-alt") shouldEqual Some(Map(21 -> 1))
    chrq.get("AD-ref") shouldEqual Some(Map(24 -> 1))
    chrq.get("general") should not be empty

    val general = chrq.get("general").get
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

    val total = blah.get("total").get
    total.get("GQ") shouldEqual Some(Map(99 -> 1))
    total.get("AD") shouldEqual Some(Map(24 -> 1, 21 -> 1))
    total.get("AD-used") shouldEqual Some(Map(24 -> 1, 21 -> 1))
    total.get("DP") shouldEqual Some(Map(45 -> 1))
    total.get("AD-alt") shouldEqual Some(Map(21 -> 1))
    total.get("AD-ref") shouldEqual Some(Map(24 -> 1))
    total.get("general") should not be empty

    val totGeneral = total.get("general").get
    totGeneral.get("Hom") shouldEqual Some(0)
    totGeneral.get("NoCall") shouldEqual Some(0)
    totGeneral.get("Variant") shouldEqual Some(1)
    totGeneral.get("Filtered") shouldEqual Some(0)
    totGeneral.get("NonInformative") shouldEqual Some(0)
    totGeneral.get("Called") shouldEqual Some(1)
    totGeneral.get("Total") shouldEqual Some(1)
    totGeneral.get("HomVar") shouldEqual Some(0)
    totGeneral.get("HomRef") shouldEqual Some(0)
    totGeneral.get("Mixed") shouldEqual Some(0)
    totGeneral.get("Available") shouldEqual Some(1)
    totGeneral.get("Het") shouldEqual Some(1)
    totGeneral.get("HetNonRef") shouldEqual Some(0)
  }
}
