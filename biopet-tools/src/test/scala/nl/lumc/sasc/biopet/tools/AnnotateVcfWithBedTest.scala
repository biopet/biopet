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
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Random
/**
 * Test class for AnnotateVcfWithBed
 *
 * Created by ahbbollen on 9-4-15.
 */
class AnnotateVcfWithBedTest extends TestNGSuite with MockitoSugar with Matchers {
  import AnnotateVcfWithBed._

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val veppedPath = resourcePath("/VEP_oneline.vcf")
  val bed = resourcePath("/rrna01.bed")
  val rand = new Random()

  @Test def testOutputTypeVcf() = {
    val tmpPath = "/tmp/VcfFilter_" + rand.nextString(10) + ".vcf"
    val arguments: Array[String] = Array("-I", veppedPath, "-o", tmpPath, "-B", bed, "-f", "testing")
    main(arguments)
  }

  @Test def testOutputTypeBcf() = {
    val tmpPath = "/tmp/VcfFilter_" + rand.nextString(10) + ".bcf"
    val arguments: Array[String] = Array("-I", veppedPath, "-o", tmpPath, "-B", bed, "-f", "testing")
    main(arguments)
  }

  @Test def testOutputTypeVcfGz() = {
    val tmpPath = "/tmp/VcfFilter_" + rand.nextString(10) + ".vcf.gz"
    val arguments: Array[String] = Array("-I", veppedPath, "-o", tmpPath, "-B", bed, "-f", "testing")
    main(arguments)
  }

}
