package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
  * Created by pjvanthof on 17/06/2017.
  */
class MultiCoverageTest extends TestNGSuite with Matchers {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testDefault(): Unit = {
    val outputFile = File.createTempFile("output.", ".txt")
    outputFile.deleteOnExit()
    MultiCoverage.main(
      Array("-L",
            resourcePath("/rrna02.bed"),
            "-b",
            resourcePath("/paired01.bam"),
            "-o",
            outputFile.getAbsolutePath))

    Source.fromFile(outputFile).getLines().toList shouldBe List(
      "#contig\tstart\tend\tWipeReadsTestCase",
      "chrQ\t300\t350\t0",
      "chrQ\t350\t400\t0",
      "chrQ\t450\t480\t9",
      "chrQ\t470\t475\t0",
      "chrQ\t1\t200\t40",
      "chrQ\t150\t250\t19"
    )
  }

  @Test
  def testMean(): Unit = {
    val outputFile = File.createTempFile("output.", ".txt")
    outputFile.deleteOnExit()
    MultiCoverage.main(
      Array("-L",
            resourcePath("/rrna02.bed"),
            "-b",
            resourcePath("/paired01.bam"),
            "-o",
            outputFile.getAbsolutePath,
            "--mean"))

    Source.fromFile(outputFile).getLines().toList shouldBe List(
      "#contig\tstart\tend\tWipeReadsTestCase",
      "chrQ\t300\t350\t0.0",
      "chrQ\t350\t400\t0.0",
      "chrQ\t450\t480\t0.3",
      "chrQ\t470\t475\t0.0",
      "chrQ\t1\t200\t0.20100502512562815",
      "chrQ\t150\t250\t0.19"
    )
  }
}
