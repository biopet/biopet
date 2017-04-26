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
import java.sql.Date

import com.google.common.io.Files
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{DataProvider, Test}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
  * Created by pjvanthof on 24/02/16.
  */
class ReportBuilderTest extends TestNGSuite with Matchers {

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @DataProvider(name = "testGeneratePages")
  def generatePageProvider = {
    val sample = Array(Some("sampleName"), None)
    val lib = Array(Some("libName"), None)
    val nested = Array(false, true)
    for (s <- sample; l <- lib; n <- nested if (!(l.isDefined && s.isEmpty))) yield Array(s, l, n)
  }

  @Test(dataProvider = "testGeneratePages")
  def testGeneratePages(sample: Option[String], lib: Option[String], nested: Boolean): Unit = {
    val builder = new ReportBuilder {
      def pipelineName = "test"
      def reportName: String = "test"
      def indexPage: Future[ReportPage] =
        Future(
          ReportPage((if (nested) "p1" -> Future(ReportPage(Nil, Nil, Map())) :: Nil else Nil),
                     Nil,
                     Map()))
    }

    import scala.concurrent.ExecutionContext.Implicits.global

    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()
    sample.foreach { sampleName =>
      val sampleId = Await.result(db.createSample(sampleName, 0), Duration.Inf)
      lib.foreach { libName =>
        Await.result(db.createLibrary(libName, 0, sampleId), Duration.Inf)
      }
    }
    Await.ready(db.createPipeline("test", 0), Duration.Inf)
    Await.ready(db.createRun("test", "", "", "", new Date(System.currentTimeMillis())),
                Duration.Inf)

    val tempDir = Files.createTempDir()
    tempDir.deleteOnExit()
    val args = Array("-s", dbFile.getAbsolutePath, "-o", tempDir.getAbsolutePath) ++
      sample.map(x => Array("-a", s"sampleId=$x")).getOrElse(Array()) ++
      lib.map(x => Array("-a", s"libId=$x")).getOrElse(Array())
    builder.main(args)
    builder.sampleId shouldBe sample.flatMap(s => Await.result(db.getSampleId(0, s), Duration.Inf))
    builder.libId shouldBe lib.flatMap(l =>
      Await.result(db.getLibraryId(0, builder.sampleId.get, l), Duration.Inf))
    builder.extFiles.foreach(x =>
      new File(tempDir, "ext" + File.separator + x.targetPath) should exist)
    new File(tempDir, "index.html") should exist
    new File(tempDir, "p1" + File.separator + "index.html").exists() shouldBe nested

    db.close()
  }

  //  @Test
  //  def testCountPages: Unit = {
  //    ReportBuilder.countPages(ReportPage(Nil, Nil, Map())) shouldBe 1
  //    ReportBuilder.countPages(ReportPage(
  //      "p1" -> ReportPage(Nil, Nil, Map()) :: Nil,
  //      Nil, Map())) shouldBe 2
  //    ReportBuilder.countPages(ReportPage(
  //      "p1" -> ReportPage(Nil, Nil, Map()) :: "p2" -> ReportPage(Nil, Nil, Map()) :: Nil,
  //      Nil, Map())) shouldBe 3
  //    ReportBuilder.countPages(ReportPage(
  //      "p1" -> ReportPage("p1" -> ReportPage(Nil, Nil, Map()) :: Nil, Nil, Map()) :: Nil,
  //      Nil, Map())) shouldBe 3
  //    ReportBuilder.countPages(ReportPage(
  //      "p1" -> ReportPage(Nil, Nil, Map()) :: "p2" -> ReportPage("p1" -> ReportPage(Nil, Nil, Map()) :: Nil, Nil, Map()) :: Nil,
  //      Nil, Map())) shouldBe 4
  //  }

  @Test
  def testRenderTemplate: Unit = {
    ReportBuilder.renderTemplate("/template.ssp", Map("arg" -> "test")) shouldBe "test"
    ReportBuilder.renderTemplate("/template.ssp", Map("arg" -> "bla")) shouldBe "bla"
  }

}
