package nl.lumc.sasc.biopet.tools

import org.scalatest.testng.TestNGSuite
import org.scalatest.mock.MockitoSugar
import org.scalatest.Matchers
import java.io.File
import java.nio.file.Paths
import org.testng.annotations.Test
import htsjdk.variant.vcf.VCFFileReader
import htsjdk.tribble.TribbleException
import scala.collection.JavaConversions._
import htsjdk.variant.variantcontext.VariantContext
import java.lang.String
import scala.Predef.String

/**
 * This class tests the VEPNormalizer
 * Created by ahbbollen on 11/24/14.
 */
class VEPNormalizerTest extends TestNGSuite with MockitoSugar with Matchers {
  import VEPNormalizer._

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val vcf3 = new File(resourcePath("/VCFv3.vcf"))
  val vepped = new File(resourcePath("/VEP_oneline.vcf"))
  val unvepped = new File(resourcePath("/unvepped.vcf"))

  @Test def testVEPHeaderLength() = {
    val reader = new VCFFileReader(vepped, false)
    val header = reader.getFileHeader
    parse_csq(header).length should be(27)
  }

  @Test def testExplodeVEPLength() = {
    val reader = new VCFFileReader(vepped, false)
    val header = reader.getFileHeader
    val new_infos = parse_csq(header)
    explode_transcripts(reader.iterator().next(), new_infos).length should be(11)
  }

  @Test def testStandardVEPLength() = {
    val reader = new VCFFileReader(vepped, false)
    val header = reader.getFileHeader
    val new_infos = parse_csq(header)
    Array(standardTranscripts(reader.iterator().next(), new_infos)).length should be(1)
  }

  @Test def testStandardVEPAttributeLength() = {
    val reader = new VCFFileReader(vepped, false)
    val header = reader.getFileHeader
    val new_infos = parse_csq(header)
    val record = standardTranscripts(reader.iterator().next(), new_infos)
    def checkItems(items: Array[String]) = {
      items.foreach { check }
    }

    def check(item: String) = {
      println(record.getAttribute(item))
      record.getAttribute(item).toString.split(""",""", -1).length should be(11)
    }

    val items = Array("AA_MAF", "AFR_MAF", "ALLELE_NUM", "AMR_MAF", "ASN_MAF", "Allele",
      "Amino_acids", "CDS_position", "CLIN_SIG", "Codons", "Consequence", "DISTANCE",
      "EA_MAF", "EUR_MAF", "Existing_variation", "Feature", "Feature_type",
      "GMAF", "Gene", "HGVSc", "HGVSp", "PUBMED", "Protein_position", "STRAND", "SYMBOL",
      "SYMBOL_SOURCE", "cDNA_position")

    checkItems(items)
  }

  @Test(expectedExceptions = Array(classOf[TribbleException.MalformedFeatureFile]))
  def testVCF3TribbleException() = {
    val reader = new VCFFileReader(vcf3, false)
  }

  @Test(expectedExceptions = Array(classOf[VEPException]))
  def testNoCSQTagException {
    csq_check(new VCFFileReader(unvepped, false).getFileHeader)
  }

}
