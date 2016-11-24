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

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by ahbbollen on 28-8-15.
 */
class SamplesTsvToConfigTest extends TestNGSuite with MockitoSugar with Matchers {
  import SamplesTsvToConfig._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testCorrectSampleTsv = {
    val tsv = resourcePath("/sample.tsv")
    val output = File.createTempFile("testCorrectSampleTsv", ".json")
    output.deleteOnExit()

    noException should be thrownBy main(Array("-i", tsv, "-o", output.toString))
  }

  @Test
  def testNoSampleColumn() = {
    val tsv = resourcePath("/no_sample.tsv")
    val output = File.createTempFile("testNoSampleColumn", ".json")
    output.deleteOnExit()
    val thrown = the[IllegalStateException] thrownBy main(Array("-i", tsv, "-o", output.toString))
    thrown.getMessage should equal("Sample column does not exist in: " + tsv)
  }

  @Test
  def testNumberInLibs = {
    val tsv = resourcePath("/number.tsv")
    val output = File.createTempFile("testNumberInLibs", ".json")
    output.deleteOnExit()
    val thrown = the[IllegalStateException] thrownBy main(Array("-i", tsv, "-o", output.toString))
    thrown.getMessage should equal("Sample or library may not start with a number")
  }

  @Test
  def testSampleIDs = {
    val tsv = resourcePath("/same.tsv")
    val output = File.createTempFile("testSampleIDs", ".json")
    output.deleteOnExit()
    val thrown = the[IllegalStateException] thrownBy main(Array("-i", tsv, "-o", output.toString))
    thrown.getMessage should equal("Combination of Sample_ID_1 and Lib_ID_1 is found multiple times")

  }

  @Test
  def testJson = {
    val tsv = new File(resourcePath("/sample.tsv"))
    val json = stringFromInputs(List(tsv), Nil)

    json should equal(
      """|{
        |  "samples" : {
        |    "Sample_ID_1" : {
        |      "libraries" : {
        |        "Lib_ID_1" : {
        |          "bam" : "MyFirst.bam"
        |        }
        |      }
        |    },
        |    "Sample_ID_2" : {
        |      "libraries" : {
        |        "Lib_ID_2" : {
        |          "bam" : "MySecond.bam"
        |        }
        |      }
        |    }
        |  }
        |}""".stripMargin)
  }

}
