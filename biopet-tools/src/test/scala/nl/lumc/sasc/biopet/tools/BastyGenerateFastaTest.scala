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
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

/**
 * Created by ahbbollen on 13-8-15.
 */
class BastyGenerateFastaTest extends TestNGSuite with MockitoSugar with Matchers {

  import BastyGenerateFasta._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val veppedPath = resourcePath("/VEP_oneline.vcf")
  val vepped = new File(veppedPath)
  val bamPath = resourcePath("/paired01.bam")
  val chrQPath = resourcePath("/chrQ.vcf.gz")
  val chrQRefPath = resourcePath("/fake_chrQ.fa")
  val bam = new File(resourcePath("/paired01.bam"))
  val chrQ = new File(resourcePath("/chrQ.vcf.gz"))
  val chrQRef = new File(resourcePath("/fake_chrQ.fa"))

  @Test def testMainVcf = {
    val tmp = File.createTempFile("basty_out", ".fa")
    tmp.deleteOnExit()
    val tmppath = tmp.getAbsolutePath
    tmp.deleteOnExit()

    val arguments = Array("-V", chrQPath, "--outputVariants", tmppath, "--sampleName", "Sample_101", "--reference", chrQRefPath, "--outputName", "test")
    main(arguments)
  }

  @Test def testMainVcfAndBam = {
    val tmp = File.createTempFile("basty_out", ".fa")
    tmp.deleteOnExit()
    val tmppath = tmp.getAbsolutePath
    tmp.deleteOnExit()

    val arguments = Array("-V", chrQPath, "--outputVariants", tmppath, "--bamFile", bamPath, "--sampleName", "Sample_101", "--reference", chrQRefPath, "--outputName", "test")
    main(arguments)
  }

  @Test def testMainVcfAndBamMore = {
    val tmp = File.createTempFile("basty_out", ".fa")
    tmp.deleteOnExit()
    val tmppath = tmp.getAbsolutePath
    tmp.deleteOnExit()

    val arguments = Array("-V", chrQPath, "--outputConsensus", tmppath, "--outputConsensusVariants", tmppath, "--bamFile", bamPath, "--sampleName", "Sample_101", "--reference", chrQRefPath, "--outputName", "test")
    main(arguments)
  }

  @Test def testGetMaxAllele = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    val one = mock[Args]
    when(one.sampleName) thenReturn "Sample_101"
    val two = mock[Args]
    when(two.sampleName) thenReturn "Sample_102"

    getMaxAllele(record)(one) shouldBe "C-"
    getMaxAllele(record)(two) shouldBe "CA"

  }

}
