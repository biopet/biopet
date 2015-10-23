package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import htsjdk.variant.vcf.VCFFileReader
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite

import GvcfToBed._
import org.testng.annotations.Test

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

    hasMinGenomeQuality(record, None, 99) shouldBe true
    hasMinGenomeQuality(record, Some("Sample_101"), 99) shouldBe true

    val reader2 = new VCFFileReader(unvepped, false)
    val record2 = reader2.iterator.next()

    hasMinGenomeQuality(record2, None, 99) shouldBe false
    hasMinGenomeQuality(record2, None, 0) shouldBe false
    hasMinGenomeQuality(record2, Some("Sample_102"), 3) shouldBe true
    hasMinGenomeQuality(record2, Some("Sample_102"), 99) shouldBe false
  }

  @Test def wrongSample = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    an [IllegalArgumentException] should be thrownBy hasMinGenomeQuality(record, Some("dummy"), 99)
  }

  @Test def testCreateBedRecord = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    val bed = createBedRecord(record)
    bed.chr shouldBe "chr1"
    bed.start shouldBe 871042
    bed.end shouldBe 871042

    val reader2 = new VCFFileReader(unvepped, false)
    val record2 = reader2.iterator.next()

    val bed2 = createBedRecord(record2)
    bed2.chr shouldBe "chr1"
    bed2.start shouldBe 14599
    bed2.end shouldBe 14599

    //TODO: add GVCF-block vcf file to test
  }

}
