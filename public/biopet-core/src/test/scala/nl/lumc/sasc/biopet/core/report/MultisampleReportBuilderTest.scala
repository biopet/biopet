package nl.lumc.sasc.biopet.core.report

import java.io.File
import java.nio.file.Paths

import com.google.common.io.Files
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvanthof on 24/02/16.
  */
class MultisampleReportBuilderTest extends TestNGSuite with Matchers {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testGeneratePages(): Unit = {
    val builder = new MultisampleReportBuilder {
      def reportName: String = "test"
      def indexPage: ReportPage = ReportPage("Samples" -> generateSamplesPage(Map()) :: Nil, Nil, Map())

      def samplePage(sampleId: String, args: Map[String, Any]): ReportPage =
        ReportPage("Libraries" -> generateLibraryPage(Map("sampleId" -> Some(sampleId))) :: Nil, Nil, Map())

      def libraryPage(sampleId: String, libraryId: String, args: Map[String, Any]) = ReportPage(Nil, Nil, Map())
    }

    val tempDir = Files.createTempDir()
    tempDir.deleteOnExit()
    val args = Array("-s", resourcePath("/empty_summary.json"), "-o", tempDir.getAbsolutePath)
    builder.main(args)
    builder.extFiles.foreach(x => new File(tempDir, "ext" + File.separator + x.targetPath) should exist)
    new File(tempDir, "index.html") should exist
  }

}
