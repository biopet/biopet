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

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Random

/**
 * Test class for [[VcfWithVcfTest]]
 *
 * Created by ahbbollen on 10-4-15.
 */
class VcfWithVcfTest extends TestNGSuite with MockitoSugar with Matchers {
  import VcfWithVcf._

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val veppedPath = resourcePath("/VEP_oneline.vcf.gz")
  val unveppedPath = resourcePath("/unvepped.vcf.gz")
  val rand = new Random()

  @Test def testOutputTypeVcf() = {
    val tmpPath = File.createTempFile("VcfWithVcf_", ".vcf").getAbsolutePath
    val arguments = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpPath, "-f", "CSQ")
    main(arguments)
  }

  @Test def testOutputTypeVcfGz() = {
    val tmpPath = File.createTempFile("VcfWithVcf_", ".vcf.gz").getAbsolutePath
    val arguments = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpPath, "-f", "CSQ")
    main(arguments)
  }

  @Test def testOutputTypeBcf() = {
    val tmpPath = File.createTempFile("VcfWithVcf_", ".bcf").getAbsolutePath
    val arguments = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpPath, "-f", "CSQ")
    main(arguments)
  }

  @Test def testOutputFieldException = {
    val tmpPath = File.createTempFile("VCFWithVCf", ".vcf").getAbsolutePath
    val args = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpPath, "-f", "CSQ:AC")
    an [IllegalArgumentException] should be thrownBy main(args)
    val thrown = the [IllegalArgumentException] thrownBy main(args)
    thrown.getMessage should equal("Field 'AC' already exists in input vcf")
  }

  @Test def testInputFieldException = {
    val tmpPath = File.createTempFile("VCFWithVCf", ".vcf").getAbsolutePath
    val args = Array("-I", unveppedPath, "-s", unveppedPath, "-o", tmpPath, "-f", "CSQ:NEW_CSQ")
    an [IllegalArgumentException] should be thrownBy main(args)
    val thrown = the [IllegalArgumentException] thrownBy main(args)
    thrown.getMessage should equal("Field 'CSQ' does not exist in secondary vcf")
  }

  @Test def testMinMethodException = {
    val tmpPath = File.createTempFile("VcfWithVcf_", ".vcf").getAbsolutePath
    val args = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpPath, "-f", "CSQ:CSQ:min")
    an [IllegalArgumentException] should be thrownBy main(args)
    val thrown = the [IllegalArgumentException] thrownBy main(args)
    thrown.getMessage should equal("Type of field CSQ is not numeric")
  }

  @Test def testMaxMethodException = {
    val tmpPath = File.createTempFile("VcfWithVcf_", ".vcf").getAbsolutePath
    val args = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpPath, "-f", "CSQ:CSQ:max")
    an [IllegalArgumentException] should be thrownBy main(args)
    val thrown = the [IllegalArgumentException] thrownBy main(args)
    thrown.getMessage should equal("Type of field CSQ is not numeric")
  }

}
