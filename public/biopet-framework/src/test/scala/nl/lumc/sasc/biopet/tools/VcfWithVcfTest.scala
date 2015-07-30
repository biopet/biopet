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

  val vepped_path = resourcePath("/VEP_oneline.vcf.gz")
  val unvepped_path = resourcePath("/unvepped.vcf.gz")
  val rand = new Random()

  @Test def testOutputTypeVcf() = {
    val tmp_path = "/tmp/VcfWithVcf_" + rand.nextString(10) + ".vcf"
    val arguments = Array("-I", unvepped_path, "-S", vepped_path, "-O", tmp_path, "-f", "CSQ")
    main(arguments)
  }

  @Test def testOutputTypeVcfGz() = {
    val tmp_path = "/tmp/VcfWithVcf_" + rand.nextString(10) + ".vcf.gz"
    val arguments = Array("-I", unvepped_path, "-S", vepped_path, "-O", tmp_path, "-f", "CSQ")
    main(arguments)
  }

  @Test def testOutputTypeBcf() = {
    val tmp_path = "/tmp/VcfWithVcf_" + rand.nextString(10) + ".bcf"
    val arguments = Array("-I", unvepped_path, "-S", vepped_path, "-O", tmp_path, "-f", "CSQ")
    main(arguments)
  }

}
