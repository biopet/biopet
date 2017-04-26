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

import nl.lumc.sasc.biopet.tools.SamplesTsvToConfig._
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import nl.lumc.sasc.biopet.utils.summary.Summary

/**
  * Created by ahbbollen on 31-8-15.
  */
class SummaryToTsvTest extends TestNGSuite with MockitoSugar with Matchers {
  import SummaryToTsv._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testMain = {
    val tsv = resourcePath("/test.summary.json")
    val output = File.createTempFile("main", "tsv")
    output.deleteOnExit()

    noException should be thrownBy main(
      Array("-s",
            tsv,
            "-p",
            "something=flexiprep:settings:skip_trim",
            "-m",
            "root",
            "-o",
            output.toString))
    noException should be thrownBy main(
      Array("-s",
            tsv,
            "-p",
            "something=flexiprep:settings:skip_trim",
            "-m",
            "sample",
            "-o",
            output.toString))
    noException should be thrownBy main(
      Array("-s",
            tsv,
            "-p",
            "something=flexiprep:settings:skip_trim",
            "-m",
            "lib",
            "-o",
            output.toString))
  }

  @Test
  def testHeader = {
    val tsv = resourcePath("/test.summary.json")
    val path = List("something=flexiprep:settings:skip_trim")

    val paths = path
      .map(x => {
        val split = x.split("=", 2)
        split(0) -> split(1).split(":")
      })
      .toMap

    createHeader(paths) should equal("\tsomething")
  }

  @Test
  def testLine = {
    val tsv = resourcePath("/test.summary.json")
    val path = List("something=flexiprep:settings:skip_trim")

    val paths = path
      .map(x => {
        val split = x.split("=", 2)
        split(0) -> split(1).split(":")
      })
      .toMap

    val summary = new Summary(new File(tsv))
    val values = fetchValues(summary, paths)

    val line = values.head._2.keys.map(x => createLine(paths, values, x)).head
    line should equal("value\t")
    val sampleValues = fetchValues(summary, paths, true, false)
    val sampleLine = sampleValues.head._2.keys.map(x => createLine(paths, sampleValues, x)).head
    sampleLine should equal("016\t")

    val libValues = fetchValues(summary, paths, false, true)
    val libLine = libValues.head._2.keys.map(x => createLine(paths, libValues, x)).head
    libLine should equal("016-L001\tfalse")
  }

}
