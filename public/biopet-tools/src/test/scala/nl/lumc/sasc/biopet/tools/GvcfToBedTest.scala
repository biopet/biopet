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

  val vepped_path = resourcePath("/VEP_oneline.vcf")

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
    val args: Array[String] = Array("-I", unvepped.getAbsolutePath, "-O", tmp.getAbsolutePath, "-S", "Sample_101",
      "--minGenomeQuality", "99")
    main(args)

    Source.fromFile(tmp).getLines().size shouldBe 0

    val tmp2 = File.createTempFile("gvcf2bedtest", ".bed")
    tmp2.deleteOnExit()
    val args2: Array[String] = Array("-I", unvepped.getAbsolutePath, "-O", tmp2.getAbsolutePath, "-S", "Sample_102",
      "--minGenomeQuality", "2")
    main(args2)

    Source.fromFile(tmp2).getLines().size shouldBe 1
  }

  @Test
  def testGvcfToBedInvertedOutput = {
    val tmp = File.createTempFile("gvcf2bedtest", ".bed")
    val tmp_inv = File.createTempFile("gvcf2bedtest", ".bed")
    tmp.deleteOnExit()
    tmp_inv.deleteOnExit()
    val args: Array[String] = Array("-I", unvepped.getAbsolutePath, "-O", tmp.getAbsolutePath, "-S", "Sample_101",
      "--minGenomeQuality", "99", "--invertedOutputBed", tmp_inv.getAbsolutePath)
    main(args)

    Source.fromFile(tmp_inv).getLines().size shouldBe 1

    val tmp2 = File.createTempFile("gvcf2bedtest", ".bed")
    val tmp2_inv = File.createTempFile("gvcf2bedtest", ".bed")
    tmp2.deleteOnExit()
    tmp2_inv.deleteOnExit()
    val args2: Array[String] = Array("-I", unvepped.getAbsolutePath, "-O", tmp.getAbsolutePath, "-S", "Sample_102",
      "--minGenomeQuality", "3", "--invertedOutputBed", tmp2_inv.getAbsolutePath)
    main(args2)

    Source.fromFile(tmp2_inv).getLines().size shouldBe 0
  }
}
