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
import java.util

import htsjdk.variant.vcf
import htsjdk.variant.vcf.VCFFileReader
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Random
import scala.collection.JavaConversions._
import nl.lumc.sasc.biopet.utils.VcfUtils.identicalVariantContext

/**
 * Test class for [[VcfWithVcfTest]]
 *
 * Created by ahbbollen on 10-4-15.
 */
class VcfWithVcfTest extends TestNGSuite with MockitoSugar with Matchers {
  import VcfWithVcf._

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val veppedPath = resourcePath("/VEP_oneline.vcf.gz")
  val unveppedPath = resourcePath("/unvep_online.vcf.gz")
  val referenceFasta = resourcePath("/fake_chrQ.fa")
  val monoPath = resourcePath("/chrQ_monoallelic.vcf.gz")
  val multiPath = resourcePath("/chrQ_multiallelic.vcf.gz")
  val rand = new Random()

  @Test
  def testOutputTypeVcf() = {
    val tmpFile = File.createTempFile("VcfWithVcf_", ".vcf")
    tmpFile.deleteOnExit()
    val arguments = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpFile.getAbsolutePath, "-f", "CSQ", "-R", referenceFasta)
    main(arguments)
  }

  @Test
  def testOutputTypeVcfGz() = {
    val tmpFile = File.createTempFile("VcfWithVcf_", ".vcf.gz")
    tmpFile.deleteOnExit()
    val arguments = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpFile.getAbsolutePath, "-f", "CSQ", "-R", referenceFasta)
    main(arguments)
  }

  @Test
  def testOutputTypeBcf() = {
    val tmpFile = File.createTempFile("VcfWithVcf_", ".bcf")
    tmpFile.deleteOnExit()
    val arguments = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpFile.getAbsolutePath, "-f", "CSQ", "-R", referenceFasta)
    main(arguments)
  }

  @Test
  def testOutputFieldException() = {
    val tmpFile = File.createTempFile("VCFWithVCf", ".vcf")
    tmpFile.deleteOnExit()
    val args = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpFile.getAbsolutePath, "-f", "CSQ:AC", "-R", referenceFasta)
    an[IllegalArgumentException] should be thrownBy main(args)
    val thrown = the[IllegalArgumentException] thrownBy main(args)
    thrown.getMessage should equal("Field 'AC' already exists in input vcf")
  }

  @Test
  def testInputFieldException() = {
    val tmpFile = File.createTempFile("VCFWithVCf", ".vcf")
    tmpFile.deleteOnExit()
    val args = Array("-I", unveppedPath, "-s", unveppedPath, "-o", tmpFile.getAbsolutePath, "-f", "CSQ:NEW_CSQ", "-R", referenceFasta)
    an[IllegalArgumentException] should be thrownBy main(args)
    val thrown = the[IllegalArgumentException] thrownBy main(args)
    thrown.getMessage should equal("Field 'CSQ' does not exist in secondary vcf")
  }

  @Test
  def testMinMethodException() = {
    val tmpFile = File.createTempFile("VcfWithVcf_", ".vcf")
    tmpFile.deleteOnExit()
    val args = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpFile.getAbsolutePath, "-f", "CSQ:CSQ:min", "-R", referenceFasta)
    an[IllegalArgumentException] should be thrownBy main(args)
    val thrown = the[IllegalArgumentException] thrownBy main(args)
    thrown.getMessage should equal("Type of field CSQ is not numeric")
  }

  @Test
  def testMaxMethodException() = {
    val tmpFile = File.createTempFile("VcfWithVcf_", ".vcf")
    tmpFile.deleteOnExit()
    val args = Array("-I", unveppedPath, "-s", veppedPath, "-o", tmpFile.getAbsolutePath, "-f", "CSQ:CSQ:max", "-R", referenceFasta)
    an[IllegalArgumentException] should be thrownBy main(args)
    val thrown = the[IllegalArgumentException] thrownBy main(args)
    thrown.getMessage should equal("Type of field CSQ is not numeric")
  }

  @Test
  def testFieldMap() = {
    val unvepReader = new VCFFileReader(new File(unveppedPath))
    val header = unvepReader.getFileHeader
    val unvepRecord = unvepReader.iterator().next()

    var fields = List(new Fields("FG", "FG"))
    fields :::= List(new Fields("FD", "FD"))
    fields :::= List(new Fields("GM", "GM"))
    fields :::= List(new Fields("GL", "GL"))
    fields :::= List(new Fields("CP", "CP"))
    fields :::= List(new Fields("CG", "CG"))
    fields :::= List(new Fields("CN", "CN"))
    fields :::= List(new Fields("DSP", "DSP"))
    fields :::= List(new Fields("AC", "AC"))
    fields :::= List(new Fields("AF", "AF"))
    fields :::= List(new Fields("AN", "AN"))
    fields :::= List(new Fields("BaseQRankSum", "BaseQRankSum"))
    fields :::= List(new Fields("DP", "DP"))
    fields :::= List(new Fields("FS", "FS"))
    fields :::= List(new Fields("MLEAC", "MLEAC"))
    fields :::= List(new Fields("MLEAF", "MLEAF"))
    fields :::= List(new Fields("MQ", "MQ"))
    fields :::= List(new Fields("MQ0", "MQ0"))
    fields :::= List(new Fields("MQRankSum", "MQRankSum"))
    fields :::= List(new Fields("QD", "QD"))
    fields :::= List(new Fields("RPA", "RPA"))
    fields :::= List(new Fields("RU", "RU"))
    fields :::= List(new Fields("ReadPosRankSum", "ReadPosRankSum"))
    fields :::= List(new Fields("VQSLOD", "VQSLOD"))
    fields :::= List(new Fields("culprit", "culprit"))

    val fieldMap = createFieldMap(fields, unvepRecord, List(unvepRecord), header)

    fieldMap("FG") shouldBe List("intron")
    fieldMap("FD") shouldBe List("unknown")
    fieldMap("GM") shouldBe List("NM_152486.2")
    fieldMap("GL") shouldBe List("SAMD11")
    fieldMap("CP") shouldBe List("0.000")
    fieldMap("CG") shouldBe List("-1.630")
    fieldMap("CN") shouldBe List("2294", "3274", "30362", "112930")
    fieldMap("DSP") shouldBe List("107")
    fieldMap("AC") shouldBe List("2")
    fieldMap("AF") shouldBe List("0.333")
    fieldMap("AN") shouldBe List("6")
    fieldMap("DP") shouldBe List("124")
    fieldMap("FS") shouldBe List("1.322")
    fieldMap("MLEAC") shouldBe List("2")
    fieldMap("MLEAF") shouldBe List("0.333")
    fieldMap("MQ") shouldBe List("60.0")
    fieldMap("MQ0") shouldBe List("0")
    fieldMap("MQRankSum") shouldBe List("-0.197")
    fieldMap("QD") shouldBe List("19.03")
    fieldMap("RPA") shouldBe List("1", "2")
    fieldMap("RU") shouldBe List("A")
    fieldMap("ReadPosRankSum") shouldBe List("-0.424")
    fieldMap("VQSLOD") shouldBe List("0.079")
    fieldMap("culprit") shouldBe List("FS")

  }

  @Test
  def testGetSecondaryRecords() = {
    val unvepRecord = new VCFFileReader(new File(unveppedPath)).iterator().next()
    val vepReader = new VCFFileReader(new File(veppedPath))
    val vepRecord = vepReader.iterator().next()

    val secRec = getSecondaryRecords(vepReader, unvepRecord, false)

    secRec.foreach(x => identicalVariantContext(x, vepRecord) shouldBe true)
  }

  @Test
  def testCreateRecord() = {
    val unvepRecord = new VCFFileReader(new File(unveppedPath)).iterator().next()
    val vepReader = new VCFFileReader(new File(veppedPath))
    val header = vepReader.getFileHeader
    val vepRecord = vepReader.iterator().next()

    val secRec = getSecondaryRecords(vepReader, unvepRecord, false)

    val fieldMap = createFieldMap(List(new Fields("CSQ", "CSQ")), vepRecord, secRec, header)
    val createdRecord = createRecord(fieldMap, unvepRecord, List(new Fields("CSQ", "CSQ")), header)
    identicalVariantContext(createdRecord, vepRecord) shouldBe true
  }

  @Test
  def testNumberA() = {
    val multiRecord = new VCFFileReader(new File(multiPath)).iterator().next()
    val monoRecord = new VCFFileReader(new File(monoPath)).iterator().next()

    val annot = numberA(multiRecord, monoRecord, "AF")
    annot shouldBe List("0.333")

  }

  @Test
  def testNumberR() = {
    val multiRecord = new VCFFileReader(new File(multiPath)).iterator().next()
    val monoRecord = new VCFFileReader(new File(monoPath)).iterator().next()
    val annot = numberR(multiRecord, monoRecord, "ALL_ALLELE")

    annot shouldBe List("C", "A")
  }

  @Test
  def testNumberAOutput() = {
    val tmpFile = File.createTempFile("numberA", ".vcf.gz")
    tmpFile.deleteOnExit()
    val arguments = Array("-I", monoPath, "-s", multiPath, "-o", tmpFile.getAbsolutePath, "-f", "AF:MULTI_AF", "-R", referenceFasta)
    main(arguments)
    val annotatedRecord = new VCFFileReader(tmpFile).iterator().next()
    annotatedRecord.getAttribute("MULTI_AF").toString shouldBe "0.333"

  }

  @Test
  def testNumberROutput() = {
    val tmpFile = File.createTempFile("numberR", ".vcf.gz")
    tmpFile.deleteOnExit()
    val arguments = Array("-I", monoPath, "-s", multiPath, "-o", tmpFile.getAbsolutePath, "-f", "ALL_ALLELE:MULTI_ALL_ALLELE", "-R", referenceFasta)
    main(arguments)
    val annotatedRecord = new VCFFileReader(tmpFile).iterator().next()
    annotatedRecord.getAttribute("MULTI_ALL_ALLELE") match {
      case l: List[_]           => l shouldBe List("C", "A")
      case u: util.ArrayList[_] => u.toList shouldBe List("C", "A")
      case _                    => throw new IllegalStateException("Not a list")
    }
  }

}
