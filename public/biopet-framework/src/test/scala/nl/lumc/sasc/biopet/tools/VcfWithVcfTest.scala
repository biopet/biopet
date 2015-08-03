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
    val tmpPath = File.createTempFile("VcfWithVcf_", ".vcf").getAbsolutePath
    val arguments = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpPath, "-f", "CSQ")
    main(arguments)
  }

  @Test def testOutputTypeBcf() = {
    val tmpPath = File.createTempFile("VcfWithVcf_", ".vcf").getAbsolutePath
    val arguments = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpPath, "-f", "CSQ")
    main(arguments)
  }

}
