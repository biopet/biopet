package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
 * Created by pjvanthof on 03/10/16.
 */
class DownloadNcbiAssemblyTest extends TestNGSuite with Matchers {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testNC_003403_1: Unit = {
    val output = File.createTempFile("test.", ".fasta")
    val outputReport = File.createTempFile("test.", ".report")
    output.deleteOnExit()
    outputReport.deleteOnExit()
    DownloadNcbiAssembly.main(Array("-a", "GCF_000844745.1",
      "-o", output.getAbsolutePath,
      "--report", outputReport.getAbsolutePath))

    Source.fromFile(output).getLines().toList shouldBe Source.fromFile(new File(resourcePath("/NC_003403.1.fasta"))).getLines().toList
    Source.fromFile(outputReport).getLines().toList shouldBe Source.fromFile(new File(resourcePath("/GCF_000844745.1.report"))).getLines().toList
  }
}
