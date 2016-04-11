package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 4/11/16.
 */
class SnptestToVcfTest extends TestNGSuite with Matchers {
  @Test
  def testSnptest(): Unit = {
    val output = File.createTempFile("test.", ".vcf.gz")
    output.deleteOnExit()
    SnptestToVcf.main(Array(
      "--inputInfo", SnptestToVcfTest.resourcePath("/test.snptest"),
      "--outputVcf", output.getAbsolutePath,
      "--referenceFasta", SnptestToVcfTest.resourcePath("/fake_chrQ.fa"),
      "--contig", "chrQ"
    ))
  }

  @Test
  def testEmptySnptest(): Unit = {
    val output = File.createTempFile("test.", ".vcf.gz")
    output.deleteOnExit()
    SnptestToVcf.main(Array(
      "--inputInfo", SnptestToVcfTest.resourcePath("/test.empty.snptest"),
      "--outputVcf", output.getAbsolutePath,
      "--referenceFasta", SnptestToVcfTest.resourcePath("/fake_chrQ.fa"),
      "--contig", "chrQ"
    ))
  }
}

object SnptestToVcfTest {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }
}
