package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 4/11/16.
 */
class GensToVcfTest extends TestNGSuite with Matchers {
  @Test
  def testGensOnly(): Unit = {
    val output = File.createTempFile("test.", ".vcf.gz")
    output.deleteOnExit()
    GensToVcf.main(Array(
      "--inputGenotypes", GensToVcfTest.resourcePath("/test.gens"),
      "--outputVcf", output.getAbsolutePath,
      "--referenceFasta", GensToVcfTest.resourcePath("/fake_chrQ.fa"),
      "--contig", "chrQ",
      "--samplesFile", GensToVcfTest.resourcePath("/gens.samples")
    ))
  }

  @Test
  def testGensInfo(): Unit = {
    val output = File.createTempFile("test.", ".vcf")
    output.deleteOnExit()
    GensToVcf.main(Array(
      "--inputGenotypes", GensToVcfTest.resourcePath("/test.gens"),
      "--inputInfo", GensToVcfTest.resourcePath("/test.gens_info"),
      "--outputVcf", output.getAbsolutePath,
      "--referenceFasta", GensToVcfTest.resourcePath("/fake_chrQ.fa"),
      "--contig", "chrQ",
      "--samplesFile", GensToVcfTest.resourcePath("/gens.samples"),
      "--sortInput"
    ))
  }

}

object GensToVcfTest {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }
}
