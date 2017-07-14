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

import htsjdk.samtools.fastq.FastqReader
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.collection.JavaConversions._

/**
  * Created by ahbbollen on 28-8-15.
  */
class PrefixFastqTest extends TestNGSuite with Matchers {

  import PrefixFastq._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val fq: String = resourcePath("/paired01a.fq")

  @Test
  def testMain(): Unit = {
    val temp = File.createTempFile("out", ".fastq")
    temp.deleteOnExit()

    val args = Array("-i", fq, "-o", temp.getAbsolutePath, "-s", "AAA")
    main(args)
  }

  @Test
  def testOutput(): Unit = {
    val temp = File.createTempFile("out", ".fastq")
    temp.deleteOnExit()

    val args = Array("-i", fq, "-o", temp.getAbsolutePath, "-s", "AAA")
    main(args)

    val reader = new FastqReader(temp)

    for (read <- reader.iterator()) {
      read.getReadString.startsWith("AAA") shouldBe true
    }
  }
}
