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
import java.nio.file.Paths

import htsjdk.variant.variantcontext.GenotypeType
import htsjdk.variant.vcf.VCFFileReader
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Random

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

  val vepped_path = resourcePath("/VEP_oneline.vcf")
  val vepped = new File(vepped_path)
  val rand = new Random()

  @Test def testOutputTypeVcf() = {
    val tmp_path = "/tmp/VcfFilter_" + rand.nextString(10) + ".vcf"
    val arguments: Array[String] = Array("-I", vepped_path, "-o", tmp_path)
    main(arguments)
  }

  @Test def testOutputTypeBcf() = {
    val tmp_path = "/tmp/VcfFilter_" + rand.nextString(10) + ".bcf"
    val arguments: Array[String] = Array("-I", vepped_path, "-o", tmp_path)
    main(arguments)
  }

  @Test def testOutputTypeVcfGz() = {
    val tmp_path = "/tmp/VcfFilter_" + rand.nextString(10) + ".vcf.gz"
    val arguments: Array[String] = Array("-I", vepped_path, "-o", tmp_path)
    main(arguments)
  }

  @Test def testHasGenotype() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasGenotype(record, List(("Child_7006504", GenotypeType.HET))) shouldBe true
    hasGenotype(record, List(("Child_7006504", GenotypeType.HOM_VAR))) shouldBe false
    hasGenotype(record, List(("Child_7006504", GenotypeType.HOM_REF))) shouldBe false
    hasGenotype(record, List(("Child_7006504", GenotypeType.NO_CALL))) shouldBe false
    hasGenotype(record, List(("Child_7006504", GenotypeType.MIXED))) shouldBe false

    hasGenotype(record, List(("Mother_7006508", GenotypeType.HET))) shouldBe false
    hasGenotype(record, List(("Mother_7006508", GenotypeType.HOM_VAR))) shouldBe false
    hasGenotype(record, List(("Mother_7006508", GenotypeType.HOM_REF))) shouldBe true
    hasGenotype(record, List(("Mother_7006508", GenotypeType.NO_CALL))) shouldBe false
    hasGenotype(record, List(("Mother_7006508", GenotypeType.MIXED))) shouldBe false

    hasGenotype(record, List(("Mother_7006508", GenotypeType.HOM_REF), ("Child_7006504", GenotypeType.HET))) shouldBe true
    hasGenotype(record, List(("Mother_7006508", GenotypeType.HET), ("Child_7006504", GenotypeType.HOM_REF))) shouldBe false
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

  @Test def testMustHaveVariant() = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    mustHaveVariant(record, List("Child_7006504")) shouldBe true
    mustHaveVariant(record, List("Child_7006504", "Father_7006506")) shouldBe true
    mustHaveVariant(record, List("Child_7006504", "Father_7006506", "Mother_7006508")) shouldBe false
  }

}
