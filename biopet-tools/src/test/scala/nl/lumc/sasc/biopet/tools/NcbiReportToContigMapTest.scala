package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvanthof on 30/05/2017.
  */
class NcbiReportToContigMapTest extends TestNGSuite with Matchers {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def test(): Unit = {
    val report = new File(resourcePath("/GCF_000844745.1.report"))
    val output = File.createTempFile("test.", ".tsv")
    output.deleteOnExit()
    NcbiReportToContigMap.main(
      Array("-a",
            report.getAbsolutePath,
            "-o",
            output.getAbsolutePath,
            "--nameHeader",
            "Sequence-Name"))
  }
}
