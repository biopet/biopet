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

import htsjdk.samtools.reference.IndexedFastaSequenceFile
import htsjdk.variant.variantcontext.Allele
import htsjdk.variant.vcf.VCFFileReader
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.collection.JavaConversions._

/**
 * Created by ahbbollen on 27-8-15.
 */
class MpileupToVcfTest extends TestNGSuite with MockitoSugar with Matchers {

  import MpileupToVcf._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val pileup = resourcePath("/paired01.pileup")

  @Test
  def testMain() = {
    val tmp = File.createTempFile("mpileup", ".vcf")
    tmp.deleteOnExit()
    val args = Array("-I", pileup, "--sample", "test", "-o", tmp.getAbsolutePath)

    main(args)
  }

  @Test
  def validateOutVcf() = {
    val tmp = File.createTempFile("mpileup", ".vcf")
    tmp.deleteOnExit()
    val args = Array("-I", pileup, "--sample", "test", "-o", tmp.getAbsolutePath, "--minDP", "1", "--minAP", "1")
    main(args)

    val vcfReader = new VCFFileReader(tmp, false)

    // VariantContexts validate on creation
    // therefore we just have to loop through them

    vcfReader.foreach(_ => 1)

  }

  @Test
  def extraValidateOutVcf() = {
    val tmp = File.createTempFile("mpileup", ".vcf")
    tmp.deleteOnExit()
    val args = Array("-I", pileup, "--sample", "test", "-o", tmp.getAbsolutePath, "--minDP", "1", "--minAP", "1")
    main(args)

    val vcfReader = new VCFFileReader(tmp, false)

    val fasta = resourcePath("/chrQ_allN.fa")

    val sequenceFile = new IndexedFastaSequenceFile(new File(fasta))
    val sequenceDict = sequenceFile.getSequenceDictionary

    for (record <- vcfReader) {
      val alleles = record.getAlleles.toSet
      var refAlleles = alleles -- record.getAlternateAlleles.toSet

      refAlleles.size should be >= 1

      val realRef = Allele.create(sequenceFile.getSubsequenceAt(record.getContig,
        record.getStart, record.getEnd).getBases, true)

      for (ref <- refAlleles) {
        record.extraStrictValidation(ref, realRef, Set(""))
      }
    }
  }
}
