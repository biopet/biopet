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
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

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
      def indexPage: Future[ReportPage] = Future(ReportPage("Samples" -> generateSamplesPage(Map()) :: Nil, Nil, Map()))

      def samplePage(sampleId: Int, args: Map[String, Any]): Future[ReportPage] =
        Future(ReportPage("Libraries" -> generateLibraryPage(Map("sampleId" -> Some(sampleId))) :: Nil, Nil, Map()))

      def libraryPage(sampleId: Int, libraryId: Int, args: Map[String, Any]) = Future(ReportPage(Nil, Nil, Map()))
    }

    import scala.concurrent.ExecutionContext.Implicits.global

    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val sample = Some("sampleName")
    val lib = Some("libName")

    sample.foreach { sampleName =>
      val sampleId = Await.result(db.createSample(sampleName, 0), Duration.Inf)
      lib.foreach { libName =>
        Await.result(db.createLibrary(libName, 0, sampleId), Duration.Inf)
      }

    }

    val tempDir = Files.createTempDir()
    tempDir.deleteOnExit()
    val args = Array("-s", dbFile.getAbsolutePath, "-o", tempDir.getAbsolutePath)
    builder.main(args)
    builder.extFiles.foreach(x => new File(tempDir, "ext" + File.separator + x.targetPath) should exist)

    def createFile(path: String*) = new File(tempDir, path.mkString(File.separator))

    createFile("index.html") should exist
    createFile("Samples", "index.html") should exist
    createFile("Samples", "sampleName", "index.html") should exist
    createFile("Samples", "sampleName", "Libraries", "index.html") should exist
    createFile("Samples", "sampleName", "Libraries", "libName", "index.html") should exist

    db.close()
  }

}
