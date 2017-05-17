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

/**
  * Created by pjvan_thof on 4/11/16.
  */
class SnptestToVcfTest extends TestNGSuite with Matchers {
  @Test
  def testSnptest(): Unit = {
    val output = File.createTempFile("test.", ".vcf.gz")
    output.deleteOnExit()
    SnptestToVcf.main(
      Array(
        "--inputInfo",
        SnptestToVcfTest.resourcePath("/test.snptest"),
        "--outputVcf",
        output.getAbsolutePath,
        "--referenceFasta",
        SnptestToVcfTest.resourcePath("/fake_chrQ.fa"),
        "--contig",
        "chrQ"
      ))
  }

  @Test
  def testEmptySnptest(): Unit = {
    val output = File.createTempFile("test.", ".vcf.gz")
    output.deleteOnExit()
    SnptestToVcf.main(
      Array(
        "--inputInfo",
        SnptestToVcfTest.resourcePath("/test.empty.snptest"),
        "--outputVcf",
        output.getAbsolutePath,
        "--referenceFasta",
        SnptestToVcfTest.resourcePath("/fake_chrQ.fa"),
        "--contig",
        "chrQ"
      ))
  }
}

object SnptestToVcfTest {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }
}
