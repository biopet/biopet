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
  * Created by ahbbollen on 27-8-15.
  */
class FastqSplitterTest extends TestNGSuite with MockitoSugar with Matchers {

  import FastqSplitter._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val fq: String = resourcePath("/paired01a.fq")

  @Test
  def testMain(): Unit = {
    val temp = File.createTempFile("out", ".fastq")
    temp.deleteOnExit()
    val args = Array("-I", fq, "-o", temp.getAbsolutePath)
    main(args)
  }

  @Test
  def testManyOutMain(): Unit = {
    val files = (0 until 10).map(_ => File.createTempFile("out", ".fastq"))
    files.foreach(_.deleteOnExit())
    var args = Array("-I", fq)
    files.foreach(x => args ++= Array("-o", x.getAbsolutePath))
    main(args)
  }

}
