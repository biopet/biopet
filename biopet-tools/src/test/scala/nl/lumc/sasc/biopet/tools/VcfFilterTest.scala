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
import java.nio.file.Paths

import htsjdk.variant.variantcontext.GenotypeType
import htsjdk.variant.vcf.VCFFileReader
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Random
import scala.collection.JavaConversions._

/**
  * Test class for [[VcfFilter]]
  *
  * Created by ahbbollen on 9-4-15.
  */
class VcfFilterTest extends TestNGSuite with MockitoSugar with Matchers {

  import VcfFilter._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val veppedPath = resourcePath("/VEP_oneline.vcf")
  val starPath = resourcePath("/star_genotype.vcf.gz")
  val vepped = new File(veppedPath)
  val star = new File(starPath)
  val rand = new Random()

  @Test def testOutputTypeVcf() = {
    val tmp = File.createTempFile("VcfFilter", ".vcf")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments: Array[String] = Array("-I", veppedPath, "-o", tmpPath)
    main(arguments)
  }

  @Test def testOutputTypeBcf() = {
    val tmp = File.createTempFile("VcfFilter", ".bcf")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments: Array[String] = Array("-I", veppedPath, "-o", tmpPath)
    main(arguments)
  }

  @Test def testOutputTypeVcfGz() = {
    val tmp = File.createTempFile("VcfFilter", ".vcf.gz")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments: Array[String] = Array("-I", veppedPath, "-o", tmpPath)
    main(arguments)
  }

  @Test def testMustHaveGenotypes() = {

    /**
      * This should simply not raise an exception
      */
    val tmp = File.createTempFile("VCfFilter", ".vcf")
    tmp.deleteOnExit()
    val arguments: Array[String] =
      Array("-I", veppedPath, "-o", tmp.getAbsolutePath, "--mustHaveGenotype", "Sample_101:HET")
    main(arguments)

    val size = new VCFFileReader(tmp, false).size
    size shouldBe 1

    val tmp2 = File.createTempFile("VcfFilter", ".vcf.gz")
    tmp2.deleteOnExit()
    val arguments2: Array[String] = Array("-I",
                                          veppedPath,
                                          "-o",
                                          tmp2.getAbsolutePath,
                                          "--mustHaveGenotype",
                                          "Sample_101:HOM_VAR")
    main(arguments2)

    val size2 = new VCFFileReader(tmp2, false).size
    size2 shouldBe 0

  }

  @Test def testHasGenotype() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasGenotype(record, List(("Sample_101", GenotypeType.HET))) shouldBe true
    hasGenotype(record, List(("Sample_101", GenotypeType.HOM_VAR))) shouldBe false
    hasGenotype(record, List(("Sample_101", GenotypeType.HOM_REF))) shouldBe false
    hasGenotype(record, List(("Sample_101", GenotypeType.NO_CALL))) shouldBe false
    hasGenotype(record, List(("Sample_101", GenotypeType.MIXED))) shouldBe false

    hasGenotype(record, List(("Sample_103", GenotypeType.HET))) shouldBe false
    hasGenotype(record, List(("Sample_103", GenotypeType.HOM_VAR))) shouldBe false
    hasGenotype(record, List(("Sample_103", GenotypeType.HOM_REF))) shouldBe true
    hasGenotype(record, List(("Sample_103", GenotypeType.NO_CALL))) shouldBe false
    hasGenotype(record, List(("Sample_103", GenotypeType.MIXED))) shouldBe false

    hasGenotype(
      record,
      List(("Sample_103", GenotypeType.HOM_REF), ("Sample_101", GenotypeType.HET))) shouldBe true
    hasGenotype(
      record,
      List(("Sample_103", GenotypeType.HET), ("Sample_101", GenotypeType.HOM_REF))) shouldBe false
  }

  @Test def testMinQualScore() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    minQualscore(record, 2000) shouldBe false
    minQualscore(record, 1000) shouldBe true

  }

  @Test def testHasNonRefCalls() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasNonRefCalls(record) shouldBe true
  }

  @Test def testHasCalls() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasCalls(record) shouldBe true
  }

  @Test def testHasMinDP() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasMinTotalDepth(record, 100) shouldBe true
    hasMinTotalDepth(record, 200) shouldBe false
  }

  @Test def testHasMinSampleDP() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasMinSampleDepth(record, 30, 1) shouldBe true
    hasMinSampleDepth(record, 30, 2) shouldBe true
    hasMinSampleDepth(record, 30, 3) shouldBe true
    hasMinSampleDepth(record, 40, 1) shouldBe true
    hasMinSampleDepth(record, 40, 2) shouldBe true
    hasMinSampleDepth(record, 40, 3) shouldBe false
    hasMinSampleDepth(record, 50, 1) shouldBe false
    hasMinSampleDepth(record, 50, 2) shouldBe false
    hasMinSampleDepth(record, 50, 3) shouldBe false
  }

  @Test def testHasMinSampleAD() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    minAlternateDepth(record, 0, 3) shouldBe true
    minAlternateDepth(record, 10, 2) shouldBe true
    minAlternateDepth(record, 10, 3) shouldBe false
    minAlternateDepth(record, 20, 1) shouldBe true
    minAlternateDepth(record, 20, 2) shouldBe false
  }

  @Test def testHasMinGQ() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    minGenomeQuality(record, 99, 1) shouldBe true
    minGenomeQuality(record, 99, 2) shouldBe true
    minGenomeQuality(record, 99, 3) shouldBe true
  }

  @Test def testMustHaveVariant() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    mustHaveVariant(record, List("Sample_101")) shouldBe true
    mustHaveVariant(record, List("Sample_101", "Sample_102")) shouldBe true
    mustHaveVariant(record, List("Sample_101", "Sample_102", "Sample_103")) shouldBe false

    an[IllegalArgumentException] shouldBe thrownBy(mustHaveVariant(record, List("notExistant")))

    val starReader = new VCFFileReader(star, false)
    starReader.iterator().foreach(x => mustHaveVariant(x, List("Sample_101")) shouldBe false)
  }

  @Test def testSameGenotype() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    notSameGenotype(record, "Sample_101", "Sample_102") shouldBe false
    notSameGenotype(record, "Sample_101", "Sample_103") shouldBe true
    notSameGenotype(record, "Sample_102", "Sample_103") shouldBe true
  }

  @Test def testfilterHetVarToHomVar() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    filterHetVarToHomVar(record, "Sample_101", "Sample_102") shouldBe true
    filterHetVarToHomVar(record, "Sample_101", "Sample_103") shouldBe true
    filterHetVarToHomVar(record, "Sample_102", "Sample_103") shouldBe true
  }

  @Test def testDeNovo() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    uniqueVariantInSample(record, "Sample_101") shouldBe false
    uniqueVariantInSample(record, "Sample_102") shouldBe false
    uniqueVariantInSample(record, "Sample_103") shouldBe false
  }

  @Test def testResToDom() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()
    val trio = new Trio("Sample_101", "Sample_102", "Sample_103")

    resToDom(record, List(trio)) shouldBe false
  }

  @Test def testTrioCompound = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()
    val trio = new Trio("Sample_101", "Sample_102", "Sample_103")

    trioCompound(record, List(trio))
  }

  @Test def testDeNovoTrio = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()
    val trio = new Trio("Sample_101", "Sample_102", "Sample_103")

    denovoTrio(record, List(trio))
  }

  @Test def testInIDSet() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    inIdSet(record, Set("rs199537431")) shouldBe true
    inIdSet(record, Set("dummy")) shouldBe false
  }

}
