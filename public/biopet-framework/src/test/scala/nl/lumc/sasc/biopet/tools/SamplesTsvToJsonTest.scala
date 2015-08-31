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
class SamplesTsvToJsonTest extends TestNGSuite with MockitoSugar with Matchers {
  import SamplesTsvToJson._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testCorrectSampleTsv = {
    val tsv = resourcePath("/sample.tsv")

    noException should be thrownBy main(Array("-i", tsv))
  }

  @Test
  def testNoSampleColumn() = {
    val tsv = resourcePath("/no_sample.tsv")
    val thrown = the[IllegalStateException] thrownBy main(Array("-i", tsv))
    thrown.getMessage should equal("Sample column does not exist in: " + tsv)
  }

  @Test
  def testNumberInLibs = {
    val tsv = resourcePath("/number.tsv")
    val thrown = the[IllegalStateException] thrownBy main(Array("-i", tsv))
    thrown.getMessage should equal("Sample or library may not start with a number")
  }

  @Test
  def testSampleIDs = {
    val tsv = resourcePath("/same.tsv")
    val thrown = the[IllegalStateException] thrownBy main(Array("-i", tsv))
    thrown.getMessage should equal("Combination of Sample_ID_1 and Lib_ID_1 is found multiple times")

  }

  @Test
  def testJson = {
    val tsv = new File(resourcePath("/sample.tsv"))
    val json = stringFromInputs(List(tsv))

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
