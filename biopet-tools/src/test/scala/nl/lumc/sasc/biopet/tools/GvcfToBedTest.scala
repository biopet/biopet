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

import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.VcfUtils
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite

import GvcfToBed._
import org.testng.annotations.Test

import scala.io.Source

/**
  * Created by ahbbollen on 13-10-15.
  */
class GvcfToBedTest extends TestNGSuite with Matchers with MockitoSugar {

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val vcf3 = new File(resourcePath("/VCFv3.vcf"))
  val vepped = new File(resourcePath("/VEP_oneline.vcf"))
  val unvepped = new File(resourcePath("/unvepped.vcf"))

  val veppedPath = resourcePath("/VEP_oneline.vcf")

  @Test def testMinQuality = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    VcfUtils.hasMinGenomeQuality(record, "Sample_101", 99) shouldBe true

    val reader2 = new VCFFileReader(unvepped, false)
    val record2 = reader2.iterator.next()

    VcfUtils.hasMinGenomeQuality(record2, "Sample_102", 3) shouldBe true
    VcfUtils.hasMinGenomeQuality(record2, "Sample_102", 99) shouldBe false
  }

  @Test
  def testGvcfToBedOutput = {
    val tmp = File.createTempFile("gvcf2bedtest", ".bed")
    tmp.deleteOnExit()
    val args: Array[String] = Array("-I",
                                    unvepped.getAbsolutePath,
                                    "-O",
                                    tmp.getAbsolutePath,
                                    "-S",
                                    "Sample_101",
                                    "--minGenomeQuality",
                                    "99")
    main(args)

    Source.fromFile(tmp).getLines().size shouldBe 0

    val tmp2 = File.createTempFile("gvcf2bedtest", ".bed")
    tmp2.deleteOnExit()
    val args2: Array[String] = Array("-I",
                                     unvepped.getAbsolutePath,
                                     "-O",
                                     tmp2.getAbsolutePath,
                                     "-S",
                                     "Sample_102",
                                     "--minGenomeQuality",
                                     "2")
    main(args2)

    Source.fromFile(tmp2).getLines().size shouldBe 1
  }

  @Test
  def testGvcfToBedInvertedOutput = {
    val tmp = File.createTempFile("gvcf2bedtest", ".bed")
    val tmpInv = File.createTempFile("gvcf2bedtest", ".bed")
    tmp.deleteOnExit()
    tmpInv.deleteOnExit()
    val args: Array[String] = Array("-I",
                                    unvepped.getAbsolutePath,
                                    "-O",
                                    tmp.getAbsolutePath,
                                    "-S",
                                    "Sample_101",
                                    "--minGenomeQuality",
                                    "99",
                                    "--invertedOutputBed",
                                    tmpInv.getAbsolutePath)
    main(args)

    Source.fromFile(tmpInv).getLines().size shouldBe 1

    val tmp2 = File.createTempFile("gvcf2bedtest", ".bed")
    val tmp2Inv = File.createTempFile("gvcf2bedtest", ".bed")
    tmp2.deleteOnExit()
    tmp2Inv.deleteOnExit()
    val args2: Array[String] = Array("-I",
                                     unvepped.getAbsolutePath,
                                     "-O",
                                     tmp.getAbsolutePath,
                                     "-S",
                                     "Sample_102",
                                     "--minGenomeQuality",
                                     "3",
                                     "--invertedOutputBed",
                                     tmp2Inv.getAbsolutePath)
    main(args2)

    Source.fromFile(tmp2Inv).getLines().size shouldBe 0
  }
}
