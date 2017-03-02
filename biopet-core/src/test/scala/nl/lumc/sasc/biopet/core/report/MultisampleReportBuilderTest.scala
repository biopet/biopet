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

      def samplePage(sampleId: Int, args: Map[String, Any]): ReportPage =
        ReportPage("Libraries" -> generateLibraryPage(Map("sampleId" -> Some(sampleId))) :: Nil, Nil, Map())

      def libraryPage(sampleId: Int, libraryId: Int, args: Map[String, Any]) = ReportPage(Nil, Nil, Map())
    }

    val tempDir = Files.createTempDir()
    tempDir.deleteOnExit()
    val args = Array("-s", resourcePath("/empty_summary.json"), "-o", tempDir.getAbsolutePath)
    builder.main(args)
    builder.extFiles.foreach(x => new File(tempDir, "ext" + File.separator + x.targetPath) should exist)

    def createFile(path: String*) = new File(tempDir, path.mkString(File.separator))

    createFile("index.html") should exist
    createFile("Samples", "index.html") should exist
    createFile("Samples", "sampleName", "index.html") should exist
    createFile("Samples", "sampleName", "Libraries", "index.html") should exist
    createFile("Samples", "sampleName", "Libraries", "libName", "index.html") should exist
  }

}
