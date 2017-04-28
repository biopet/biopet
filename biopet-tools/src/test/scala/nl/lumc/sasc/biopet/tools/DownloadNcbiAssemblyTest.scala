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
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
  * Created by pjvanthof on 03/10/16.
  */
class DownloadNcbiAssemblyTest extends TestNGSuite with Matchers {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testNC_003403_1: Unit = {
    val output = File.createTempFile("test.", ".fasta")
    val outputReport = File.createTempFile("test.", ".report")
    output.deleteOnExit()
    outputReport.deleteOnExit()
    DownloadNcbiAssembly.main(
      Array("-a",
            new File(resourcePath("/GCF_000844745.1.report")).getAbsolutePath,
            "-o",
            output.getAbsolutePath,
            "--report",
            outputReport.getAbsolutePath))

    Source.fromFile(output).getLines().toList shouldBe Source
      .fromFile(new File(resourcePath("/NC_003403.1.fasta")))
      .getLines()
      .toList
    Source.fromFile(outputReport).getLines().toList shouldBe Source
      .fromFile(new File(resourcePath("/GCF_000844745.1.report")))
      .getLines()
      .toList
  }
}
