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

import htsjdk.samtools.{SamReaderFactory, SamReader}
import htsjdk.variant.vcf.VCFFileReader
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Random

/**
  * Test class for [[CheckAllelesVcfInBam]]
  *
  * Created by ahbbollen on 10-4-15.
  */
class CheckAllelesVcfInBamTest extends TestNGSuite with MockitoSugar with Matchers {
  import CheckAllelesVcfInBam._

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val vcf: String = resourcePath("/chrQ.vcf")
  val bam: String = resourcePath("/single01.bam")
  val vcf2 = new File(resourcePath("/chrQ2.vcf.gz"))
  val rand = new Random()

  @Test def testOutputTypeVcf(): Unit = {
    val tmp = File.createTempFile("CheckAllelesVcfInBam", ".vcf")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments = Array("-I", vcf, "-b", bam, "-s", "sample01", "-o", tmpPath)
    main(arguments)
  }

  @Test def testOutputTypeVcfGz(): Unit = {
    val tmp = File.createTempFile("CheckAllelesVcfInBam", ".vcf.gz")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments = Array("-I", vcf, "-b", bam, "-s", "sample01", "-o", tmpPath)
    main(arguments)
  }

  @Test def testOutputTypeBcf(): Unit = {
    val tmp = File.createTempFile("CheckAllelesVcfInBam", ".bcf")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments = Array("-I", vcf, "-b", bam, "-s", "sample01", "-o", tmpPath)
    main(arguments)
  }

  @Test
  def testCheckAllelesNone(): Unit = {
    val variant = new File(vcf)
    val samRecord = SamReaderFactory.makeDefault().open(new File(bam)).iterator().next()
    val varRecord = new VCFFileReader(variant, false).iterator().next()
    checkAlleles(samRecord, varRecord) shouldBe None
  }

  @Test
  def testCheckAlleles(): Unit = {
    val samRecord = SamReaderFactory.makeDefault().open(new File(bam)).iterator().next()
    val varRecord = new VCFFileReader(vcf2).iterator().next()
    checkAlleles(samRecord, varRecord) shouldBe Some("T")
  }

}
