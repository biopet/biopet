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
package nl.lumc.sasc.biopet.utils.summary

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvanthof on 06/05/16.
  */
class SummaryValueTest extends TestNGSuite with Matchers {
  @Test
  def testConstructor(): Unit = {
    val summary = new Summary(SummaryValueTest.testSummaryFile)
    SummaryValue(None).value shouldBe None
    SummaryValue(Some(1)).value shouldBe Some(1)
    new SummaryValue(List("key_1"), summary, None, None).value shouldBe Some("test_1")
    new SummaryValue(List("key_2"), summary, Some("sample1"), None).value shouldBe Some("test_2")
    new SummaryValue(List("key_3"), summary, Some("sample1"), Some("lib1")).value shouldBe Some(
      "test_3")
  }

  @Test
  def testPlus(): Unit = {
    SummaryValue(Some(1.0)) + SummaryValue(Some(1.0)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(Some(1.0)) + SummaryValue(Some(1)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(Some(1)) + SummaryValue(Some(1)) shouldBe SummaryValue(Some(2))
    SummaryValue(Some("1")) + SummaryValue(Some(1)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(Some("1")) + SummaryValue(Some(1.0)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(None) + SummaryValue(Some(1.0)) shouldBe SummaryValue(None)
  }

  @Test
  def testMin(): Unit = {
    SummaryValue(Some(1.0)) - SummaryValue(Some(1.0)) shouldBe SummaryValue(Some(0.0))
    SummaryValue(Some(1.0)) - SummaryValue(Some(1)) shouldBe SummaryValue(Some(0.0))
    SummaryValue(Some(1)) - SummaryValue(Some(1)) shouldBe SummaryValue(Some(0))
    SummaryValue(Some("1")) - SummaryValue(Some(1)) shouldBe SummaryValue(Some(0.0))
    SummaryValue(Some("1")) - SummaryValue(Some(1.0)) shouldBe SummaryValue(Some(0.0))
    SummaryValue(None) - SummaryValue(Some(1.0)) shouldBe SummaryValue(None)
  }

  @Test
  def testMultiply(): Unit = {
    SummaryValue(Some(1.0)) * SummaryValue(Some(2.0)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(Some(1.0)) * SummaryValue(Some(2)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(Some(1)) * SummaryValue(Some(2)) shouldBe SummaryValue(Some(2))
    SummaryValue(Some("1")) * SummaryValue(Some(2)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(Some("1")) * SummaryValue(Some(2.0)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(None) * SummaryValue(Some(2.0)) shouldBe SummaryValue(None)
  }

  @Test
  def testDivide(): Unit = {
    SummaryValue(Some(2.0)) / SummaryValue(Some(1.0)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(Some(2.0)) / SummaryValue(Some(1)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(Some(2)) / SummaryValue(Some(1)) shouldBe SummaryValue(Some(2))
    SummaryValue(Some("2")) / SummaryValue(Some(1)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(Some("2")) / SummaryValue(Some(1.0)) shouldBe SummaryValue(Some(2.0))
    SummaryValue(None) / SummaryValue(Some(1.0)) shouldBe SummaryValue(None)
  }

  @Test
  def testLeft(): Unit = {
    SummaryValue(Some(2.0)) % SummaryValue(Some(1.0)) shouldBe SummaryValue(Some(0))
    SummaryValue(Some(2.0)) % SummaryValue(Some(1)) shouldBe SummaryValue(Some(0))
    SummaryValue(Some(2)) % SummaryValue(Some(1)) shouldBe SummaryValue(Some(0))
    SummaryValue(Some("2")) % SummaryValue(Some(1)) shouldBe SummaryValue(Some(0))
    SummaryValue(Some("2")) % SummaryValue(Some(1.0)) shouldBe SummaryValue(Some(0))
    SummaryValue(None) % SummaryValue(Some(1.0)) shouldBe SummaryValue(None)
  }

}

object SummaryValueTest {
  val testSummary = Map(
    "key_1" -> "test_1",
    "samples" -> Map(
      "sample1" -> Map(
        "key_2" -> "test_2",
        "libraries" -> Map(
          "lib1" -> Map("key_3" -> "test_3")
        )
      )
    )
  )

  val testSummaryFile: File = File.createTempFile("summary.", ".json")
  testSummaryFile.deleteOnExit()

  val writer = new PrintWriter(testSummaryFile)
  writer.println(ConfigUtils.mapToJson(testSummary).nospaces)
  writer.close()
}
