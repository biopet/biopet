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
class SummaryTest extends TestNGSuite with Matchers {
  @Test
  def testSamples: Unit = {
    val summary = new Summary(SummaryTest.testSummaryFile)
    summary.samples shouldBe Set("sample1")
  }

  @Test
  def testLibraries: Unit = {
    val summary = new Summary(SummaryTest.testSummaryFile)
    summary.libraries shouldBe Map("sample1" -> Set("lib1"))
  }

  @Test
  def testValue: Unit = {
    val summary = new Summary(SummaryTest.testSummaryFile)
    summary.getValue("key_1") shouldBe Some("test_1")
    summary.getValue("key_x") shouldBe None
    summary.getValue("samples", "sample1", "key_2") shouldBe Some("test_2")
    summary.getValue("samples", "sample1", "libraries", "lib1", "key_3") shouldBe Some("test_3")

    summary.getValue(Some("sample1"), None, "key_2") shouldBe Some("test_2")
    summary.getValue(Some("sample1"), None, "key_x") shouldBe None
    summary.getValue(Some("sample1"), Some("lib1"), "key_3") shouldBe Some("test_3")
    summary.getValue(Some("sample1"), Some("lib1"), "key_x") shouldBe None
    summary.getValue(None, None, "key_1") shouldBe Some("test_1")
    summary.getValue(None, None, "key_x") shouldBe None
  }

  @Test
  def testSampleValue: Unit = {
    val summary = new Summary(SummaryTest.testSummaryFile)
    summary.getSampleValue("sample1", "key_2") shouldBe Some("test_2")
    summary.getSampleValue("sample1", "key_x") shouldBe None
    summary.getSampleValue("samplex", "key_x") shouldBe None
  }

  @Test
  def testSampleValues: Unit = {
    val summary = new Summary(SummaryTest.testSummaryFile)
    summary.getSampleValues("key_2") shouldBe Map("sample1" -> Some("test_2"))
    summary.getSampleValues("key_x") shouldBe Map("sample1" -> None)
    summary.getSampleValues((summary, sample) => summary.getSampleValue(sample, "key_2")) shouldBe Map(
      "sample1" -> Some("test_2"))

  }

  @Test
  def testLibraryValue: Unit = {
    val summary = new Summary(SummaryTest.testSummaryFile)
    summary.getLibraryValue("sample1", "lib1", "key_3") shouldBe Some("test_3")
    summary.getLibraryValue("sample1", "lib1", "key_x") shouldBe None
    summary.getLibraryValue("samplex", "libx", "key_x") shouldBe None
  }

  @Test
  def testLibraryValues: Unit = {
    val summary = new Summary(SummaryTest.testSummaryFile)
    summary.getLibraryValues("key_3") shouldBe Map(("sample1", "lib1") -> Some("test_3"))
    summary.getLibraryValues("key_x") shouldBe Map(("sample1", "lib1") -> None)
    summary.getLibraryValues((summary, sample, lib) =>
      summary.getLibraryValue(sample, lib, "key_3")) shouldBe Map(
      ("sample1", "lib1") -> Some("test_3"))
  }

}

object SummaryTest {
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

  val testSummaryFile = File.createTempFile("summary.", ".json")
  testSummaryFile.deleteOnExit()

  val writer = new PrintWriter(testSummaryFile)
  writer.println(ConfigUtils.mapToJson(testSummary).nospaces)
  writer.close()
}
