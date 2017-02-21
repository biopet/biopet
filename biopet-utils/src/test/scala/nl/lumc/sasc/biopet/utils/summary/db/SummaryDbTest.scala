package nl.lumc.sasc.biopet.utils.summary.db

import java.io.File
import java.sql.Date

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Created by pjvanthof on 14/02/2017.
 */
class SummaryDbTest extends TestNGSuite with Matchers {

  @Test
  def testSettings: Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    Await.result(db.createOrUpdateSetting(0, 0, None, None, None, """{"content": "test" }"""), Duration.Inf)
    val bla = Await.result(db.getSettings(Some(0)), Duration.Inf)
    Await.result(db.getSetting(0, Left(0), None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test"))
    Await.result(db.createOrUpdateSetting(0, 0, None, None, None, """{"content": "test2" }"""), Duration.Inf)
    Await.result(db.getSetting(0, Left(0), None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test2"))
    db.close()
  }

  @Test
  def testStats: Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val runId = Await.result(db.createRun("test", "", "", "", new Date(System.currentTimeMillis())), Duration.Inf)
    val pipelineId = Await.result(db.createPipeline("test_pipeline", runId), Duration.Inf)
    val moduleId = Await.result(db.createModule("test_module", runId, pipelineId), Duration.Inf)
    val sampleId = Await.result(db.createSample("test_sample", runId), Duration.Inf)
    val libraryId = Await.result(db.createLibrary("test_library", runId, sampleId), Duration.Inf)

    Await.result(db.createOrUpdateStat(runId, pipelineId, None, None, None, """{"content": "test" }"""), Duration.Inf)
    Await.result(db.getStat(runId, Left(pipelineId), None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test"))
    Await.result(db.createOrUpdateStat(runId, pipelineId, None, None, None, """{"content": "test2" }"""), Duration.Inf)
    Await.result(db.getStat(runId, Left(pipelineId), None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test2"))

    // Test join queries
    Await.result(db.createOrUpdateStat(runId, pipelineId, Some(moduleId), Some(sampleId), Some(libraryId), """{"content": "test3" }"""), Duration.Inf)
    Await.result(db.getStat(runId, Right("test_pipeline"), Some(Right("test_module")), Some(Right("test_sample")), Some(Right("test_library"))), Duration.Inf) shouldBe Some(Map("content" -> "test3"))

    db.close()
  }

  @Test
  def testExecutable: Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    Await.result(db.createOrUpdateExecutable(0, "name"), Duration.Inf)
    val bla1 = Await.result(db.getExecutables(Some(0)), Duration.Inf)
    Await.result(db.createOrUpdateExecutable(0, "name", Some("test")), Duration.Inf)
    Await.result(db.getExecutables(Some(0)), Duration.Inf).head shouldBe Schema.Executable(0, "name", Some("test"))
    db.close()
  }

}
