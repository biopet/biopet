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

import htsjdk.tribble.TribbleException
import htsjdk.variant.vcf.VCFFileReader
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Random

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

  val veppedPath = resourcePath("/VEP_oneline.vcf")

  val rand = new Random()

  @Test def testGzOutputExplode(): Unit = {
    val tmpFile = File.createTempFile("VepNormalizer_", ".vcf.gz")
    tmpFile.deleteOnExit()
    val arguments: Array[String] = Array("-I", veppedPath, "-O", tmpFile.getAbsolutePath, "-m", "explode")
    main(arguments)
  }

  @Test def testVcfOutputExplode(): Unit = {
    val tmpFile = File.createTempFile("VepNormalizer_", ".vcf")
    tmpFile.deleteOnExit()
    val arguments: Array[String] = Array("-I", veppedPath, "-O", tmpFile.getAbsolutePath, "-m", "explode")
    main(arguments)
  }

  @Test def testBcfOutputExplode(): Unit = {
    val tmpFile = File.createTempFile("VepNormalizer_", ".bcf")
    tmpFile.deleteOnExit()
    val arguments: Array[String] = Array("-I", veppedPath, "-O", tmpFile.getAbsolutePath, "-m", "explode")
    main(arguments)
  }

  @Test def testGzOutputStandard(): Unit = {
    val tmpFile = File.createTempFile("VepNormalizer_", ".vcf.gz")
    tmpFile.deleteOnExit()
    val arguments: Array[String] = Array("-I", veppedPath, "-O", tmpFile.getAbsolutePath, "-m", "standard")
    main(arguments)
  }

  @Test def testVcfOutputStandard(): Unit = {
    val tmpFile = File.createTempFile("VepNormalizer_", ".vcf")
    tmpFile.deleteOnExit()
    val arguments: Array[String] = Array("-I", veppedPath, "-O", tmpFile.getAbsolutePath, "-m", "standard")
    main(arguments)
  }

  @Test def testBcfOutputStandard(): Unit = {
    val tmpFile = File.createTempFile("VepNormalizer_", ".bcf")
    tmpFile.deleteOnExit()
    val arguments: Array[String] = Array("-I", veppedPath, "-O", tmpFile.getAbsolutePath, "-m", "standard")
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
    val newInfos = parseCsq(header)
    explodeTranscripts(reader.iterator().next(), newInfos, removeCsq = true).length should be(11)
  }

  @Test def testStandardVEPLength() = {
    val reader = new VCFFileReader(vepped, false)
    val header = reader.getFileHeader
    val newInfos = parseCsq(header)
    Array(standardTranscripts(reader.iterator().next(), newInfos, removeCsq = true)).length should be(1)
  }

  @Test def testStandardVEPAttributeLength() = {
    val reader = new VCFFileReader(vepped, false)
    val header = reader.getFileHeader
    val newInfos = parseCsq(header)
    val record = standardTranscripts(reader.iterator().next(), newInfos, removeCsq = true)
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
