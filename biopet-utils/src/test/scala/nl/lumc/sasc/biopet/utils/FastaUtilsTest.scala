package nl.lumc.sasc.biopet.utils

import java.io.{File, PrintWriter}
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

class FastaUtilsTest extends TestNGSuite with Matchers {

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testRebuildContigMap(): Unit = {
    val inputFile = File.createTempFile("contigMap.", ".tsv")
    inputFile.deleteOnExit()

    val writer = new PrintWriter(inputFile)
    writer.println("chrT\tchrQ")
    writer.close()

    val referenceFile = new File(resourcePath("/fake_chrQ.fa"))

    val map = FastaUtils.rebuildContigMap(inputFile, referenceFile)

    map shouldBe Map("chrQ" -> Set("chrT"))
  }

  @Test
  def testReadContigMapReverse(): Unit = {
    val inputFile = File.createTempFile("contigMap.", ".tsv")
    inputFile.deleteOnExit()

    val writer = new PrintWriter(inputFile)
    writer.println("chrT\tchrQ")
    writer.close()

    val map = FastaUtils.readContigMapReverse(inputFile)

    map shouldBe Map("chrQ" -> "chrT")
  }

  @Test
  def testGetSequenceGc(): Unit = {
    val referenceFile = new File(resourcePath("/fake_chrQ.fa"))

    FastaUtils.getSequenceGc(referenceFile, "chrQ", 11, 20) shouldBe 0.4
  }

  @Test
  def testReadContigMap(): Unit = {
    val inputFile = File.createTempFile("contigMap.", ".tsv")
    inputFile.deleteOnExit()

    val writer = new PrintWriter(inputFile)
    writer.println("chrT\tchrQ")
    writer.println("chrX\t")
    writer.close()

    val map = FastaUtils.readContigMap(inputFile)

    map shouldBe Map("chrT" -> Set("chrQ"), "chrX" -> Set())
  }
}
