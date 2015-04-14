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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.tools

import scala.util.Random
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

/**
 * This class tests the VEPNormalizer
 * Created by ahbbollen on 11/24/14.
 */
class VepNormalizerTest extends TestNGSuite with MockitoSugar with Matchers {
  import VepNormalizer._

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val vcf3 = new File(resourcePath("/VCFv3.vcf"))
  val vepped = new File(resourcePath("/VEP_oneline.vcf"))
  val unvepped = new File(resourcePath("/unvepped.vcf"))

  val vepped_path = resourcePath("/VEP_oneline.vcf")

  val rand = new Random()

  @Test def testGzOutputExplode(): Unit = {
    val tmp_path = "/tmp/VepNorm_" + rand.nextString(10) + ".vcf.gz"
    val arguments: Array[String] = Array("-I", vepped_path, "-O", tmp_path, "-m", "explode")
    main(arguments)
  }

  @Test def testVcfOutputExplode(): Unit = {
    val tmp_path = "/tmp/VepNorm_" + rand.nextString(10) + ".vcf"
    val arguments: Array[String] = Array("-I", vepped_path, "-O", tmp_path, "-m", "explode")
    main(arguments)
  }

  @Test def testBcfOutputExplode(): Unit = {
    val tmp_path = "/tmp/VepNorm_" + rand.nextString(10) + ".bcf"
    val arguments: Array[String] = Array("-I", vepped_path, "-O", tmp_path, "-m", "explode")
    main(arguments)
  }

  @Test def testGzOutputStandard(): Unit = {
    val tmp_path = "/tmp/VepNorm_" + rand.nextString(10) + ".vcf.gz"
    val arguments: Array[String] = Array("-I", vepped_path, "-O", tmp_path, "-m", "standard")
    main(arguments)
  }

  @Test def testVcfOutputStandard(): Unit = {
    val tmp_path = "/tmp/VepNorm_" + rand.nextString(10) + ".vcf"
    val arguments: Array[String] = Array("-I", vepped_path, "-O", tmp_path, "-m", "standard")
    main(arguments)
  }

  @Test def testBcfOutputStandard(): Unit = {
    val tmp_path = "/tmp/VepNorm_" + rand.nextString(10) + ".bcf"
    val arguments: Array[String] = Array("-I", vepped_path, "-O", tmp_path, "-m", "standard")
    main(arguments)
  }

  @Test def testVEPHeaderLength() = {
    val reader = new VCFFileReader(vepped, false)
    val header = reader.getFileHeader
    parseCsq(header).length should be(27)
  }

  @Test def testExplodeVEPLength() = {
    val reader = new VCFFileReader(vepped, false)
    val header = reader.getFileHeader
    val new_infos = parseCsq(header)
    explodeTranscripts(reader.iterator().next(), new_infos, true).length should be(11)
  }

  @Test def testStandardVEPLength() = {
    val reader = new VCFFileReader(vepped, false)
    val header = reader.getFileHeader
    val new_infos = parseCsq(header)
    Array(standardTranscripts(reader.iterator().next(), new_infos, true)).length should be(1)
  }

  @Test def testStandardVEPAttributeLength() = {
    val reader = new VCFFileReader(vepped, false)
    val header = reader.getFileHeader
    val new_infos = parseCsq(header)
    val record = standardTranscripts(reader.iterator().next(), new_infos, true)
    def checkItems(items: Array[String]) = {
      items.foreach { check }
    }

    def check(item: String) = {
      record.getAttribute(item) match {
        case l: List[_] => l.length should be(11)
        case _          =>
      }
    }

    val items = Array("AA_MAF", "AFR_MAF", "ALLELE_NUM", "AMR_MAF", "ASN_MAF", "Allele",
      "Amino_acids", "CDS_position", "CLIN_SIG", "Codons", "Consequence", "DISTANCE",
      "EA_MAF", "EUR_MAF", "Existing_variation", "Feature", "Feature_type",
      "GMAF", "Gene", "HGVSc", "HGVSp", "PUBMED", "Protein_position", "STRAND", "SYMBOL",
      "SYMBOL_SOURCE", "cDNA_position").map("VEP_" + _)

    checkItems(items)
  }

  @Test(expectedExceptions = Array(classOf[TribbleException.MalformedFeatureFile]))
  def testVCF3TribbleException() = {
    val reader = new VCFFileReader(vcf3, false)
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def testNoCSQTagException() {
    csqCheck(new VCFFileReader(unvepped, false).getFileHeader)
  }

}
