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

    hasGenotype(record, List("Child_7006504:HET")) shouldBe true
    hasGenotype(record, List("Child_7006504:HOM_VAR")) shouldBe false
    hasGenotype(record, List("Child_7006504:HOM_REF")) shouldBe false
    hasGenotype(record, List("Child_7006504:NO_CALL")) shouldBe false
    hasGenotype(record, List("Child_7006504:MIXED")) shouldBe false

    hasGenotype(record, List("Mother_7006508:HET")) shouldBe false
    hasGenotype(record, List("Mother_7006508:HOM_VAR")) shouldBe false
    hasGenotype(record, List("Mother_7006508:HOM_REF")) shouldBe true
    hasGenotype(record, List("Mother_7006508:NO_CALL")) shouldBe false
    hasGenotype(record, List("Mother_7006508:MIXED")) shouldBe false

    hasGenotype(record, List("Mother_7006508:HOM_REF", "Child_7006504:HET")) shouldBe true
    hasGenotype(record, List("Mother_7006508:HET", "Child_7006504:HOM_HET")) shouldBe false
  }

}
