package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

class RebuildContigMapTest extends TestNGSuite with Matchers {

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testMain(): Unit = {
    val outputFile = File.createTempFile("contigMap.", ".tsv")
    outputFile.deleteOnExit()
    val inputFile = File.createTempFile("contigMap.", ".tsv")
    inputFile.deleteOnExit()

    val writer = new PrintWriter(inputFile)
    writer.println("chrT\tchrQ")
    writer.close()

    RebuildContigMap.main(Array("-I", inputFile.getAbsolutePath, "-o", outputFile.getAbsolutePath, "-R", resourcePath("/fake_chrQ.fa")))

    val reader = Source.fromFile(outputFile)
    reader.getLines().toList.filter(!_.startsWith("#")) shouldBe List("chrQ\tchrT")
    reader.close()

  }
}
