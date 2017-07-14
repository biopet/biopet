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

import scala.util.Random

/**
  * Test class for [[VcfToTsv]]
  *
  * Created by ahbbollen on 13-4-15.
  */
class VcfToTsvTest extends TestNGSuite with MockitoSugar with Matchers {
  import VcfToTsv._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val rand = new Random()

  val vepped: String = resourcePath("/VEP_oneline.vcf")
  val unvepped: String = resourcePath("/unvepped.vcf")

  @Test def testAllFields(): Unit = {
    val tmp = File.createTempFile("VcfToTsv", ".tsv")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments = Array("-I", unvepped, "-o", tmpPath, "--all_info")
    main(arguments)
  }

  @Test def testSpecificField(): Unit = {
    val tmp = File.createTempFile("VcfToTsv", ".tsv")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments = Array("-I", vepped, "-o", tmpPath, "-i", "CSQ")
    main(arguments)
  }

  @Test def testNewSeparators(): Unit = {
    val tmp = File.createTempFile("VcfToTsv", ".tsv")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments =
      Array("-I", vepped, "-o", tmpPath, "--all_info", "--separator", ",", "--list_separator", "|")
    main(arguments)
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def testIdenticalSeparators(): Unit = {
    val tmpPath = "/tmp/VcfToTsv_" + rand.nextString(10) + ".tsv"
    val arguments = Array("-I", vepped, "-o", tmpPath, "--all_info", "--separator", ",")
    main(arguments)
  }

  @Test def testFormatter(): Unit = {
    val formatter = createFormatter(2)
    formatter.format(5000.12345) should be("5000.12")
    val nformatter = createFormatter(3)
    nformatter.format(5000.12345) should be("5000.123")
  }

  @Test def testSortFields(): Unit = {
    val unsortedFields = Set("Child01-GT",
                             "Mother02-GT",
                             "Father03-GT",
                             "INFO-Something",
                             "INFO-ScoreSomething",
                             "INFO-AlleleScoreSomething",
                             "WeirdField")
    val samples = List("Child01", "Father03", "Mother02")

    val sorted = sortFields(unsortedFields, samples)
    sorted should be(
      List("WeirdField",
           "INFO-AlleleScoreSomething",
           "INFO-ScoreSomething",
           "INFO-Something",
           "Child01-GT",
           "Father03-GT",
           "Mother02-GT"))
  }

}
