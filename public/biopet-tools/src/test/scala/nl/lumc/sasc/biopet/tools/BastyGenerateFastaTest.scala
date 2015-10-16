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

  val vepped_path = resourcePath("/VEP_oneline.vcf")
  val vepped = new File(vepped_path)
  val bam_path = resourcePath("/paired01.bam")
  val chrQ_path = resourcePath("/chrQ.vcf.gz")
  val chrQRef_path = resourcePath("/fake_chrQ.fa")
  val bam = new File(resourcePath("/paired01.bam"))
  val chrQ = new File(resourcePath("/chrQ.vcf.gz"))
  val chrQRef = new File(resourcePath("/fake_chrQ.fa"))

  @Test def testMainVcf = {
    val tmp = File.createTempFile("basty_out", ".fa")
    tmp.deleteOnExit()
    val tmppath = tmp.getAbsolutePath
    tmp.deleteOnExit()

    val arguments = Array("-V", chrQ_path, "--outputVariants", tmppath, "--sampleName", "Child_7006504", "--reference", chrQRef_path, "--outputName", "test")
    main(arguments)
  }

  @Test def testMainVcfAndBam = {
    val tmp = File.createTempFile("basty_out", ".fa")
    tmp.deleteOnExit()
    val tmppath = tmp.getAbsolutePath
    tmp.deleteOnExit()

    val arguments = Array("-V", chrQ_path, "--outputVariants", tmppath, "--bamFile", bam_path, "--sampleName", "Child_7006504", "--reference", chrQRef_path, "--outputName", "test")
    main(arguments)
  }

  @Test def testMainVcfAndBamMore = {
    val tmp = File.createTempFile("basty_out", ".fa")
    tmp.deleteOnExit()
    val tmppath = tmp.getAbsolutePath
    tmp.deleteOnExit()

    val arguments = Array("-V", chrQ_path, "--outputConsensus", tmppath, "--outputConsensusVariants", tmppath, "--bamFile", bam_path, "--sampleName", "Child_7006504", "--reference", chrQRef_path, "--outputName", "test")
    main(arguments)
  }

  @Test def testGetMaxAllele = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    val child = mock[Args]
    when(child.sampleName) thenReturn "Child_7006504"
    val father = mock[Args]
    when(father.sampleName) thenReturn "Father_7006506"

    getMaxAllele(record)(child) shouldBe "C-"
    getMaxAllele(record)(father) shouldBe "CA"

  }

}
