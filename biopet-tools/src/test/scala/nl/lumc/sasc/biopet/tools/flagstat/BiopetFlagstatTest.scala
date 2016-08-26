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
package nl.lumc.sasc.biopet.tools.flagstat

import java.io.File
import java.nio.file.Paths

import htsjdk.samtools.SamReaderFactory
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
 * Created by ahbbollen on 26-8-15.
 */
class BiopetFlagstatTest extends TestNGSuite with MockitoSugar with Matchers {

  import BiopetFlagstat._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val bam = new File(resourcePath("/paired01.bam"))
  val report = new File(resourcePath("/flagstat_report.txt"))
  val summary = new File(resourcePath("/flagstat_summary.txt"))
  val crossReport = new File(resourcePath("/flagstat_crossreport.txt"))
  val crossTrue = new File(resourcePath("/flagstat_crosstrue.txt"))

  val record = SamReaderFactory.makeDefault().open(bam).iterator().next()
  val processor = new FlagstatCollector
  processor.loadDefaultFunctions()
  processor.loadRecord(record)

  @Test
  def testReport() = {
    processor.report shouldBe Source.fromFile(report).mkString
  }

  @Test
  def testSummary() = {
    processor.summary shouldBe Source.fromFile(summary).mkString
  }

  @Test
  def testCrossReport() = {
    processor.crossReport() shouldBe Source.fromFile(crossReport).mkString
  }

  @Test
  def testCrossReportTrue() = {
    processor.crossReport(true) shouldBe Source.fromFile(crossTrue).mkString
  }

  @Test
  def testMain() = {
    //TODO: Test output file
    val output = File.createTempFile("testMain", ".biopetflagstat")
    output.deleteOnExit()
    main(Array("-I", bam.getAbsolutePath, "-o", output.toString))
  }

}
