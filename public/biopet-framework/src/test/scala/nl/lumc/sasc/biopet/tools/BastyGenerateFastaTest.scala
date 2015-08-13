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
