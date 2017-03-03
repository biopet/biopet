package nl.lumc.sasc.biopet.utils.summary.db

import java.io.File
import java.sql.Date

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import scalaz._
import Scalaz._

/**
 * Testing voor [[SummaryDb]]
 * Created by pjvanthof on 14/02/2017.
 */
class SummaryDbTest extends TestNGSuite with Matchers {

  @Test
  def testRuns(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val date = new Date(System.currentTimeMillis())

    Await.result(db.getRuns(), Duration.Inf) shouldBe empty
    val runId = Await.result(db.createRun("name", "dir", "version", "hash", date), Duration.Inf)
    Await.result(db.getRuns(), Duration.Inf) shouldBe Seq(Schema.Run(runId, "name", "dir", "version", "hash", date))
    val runId2 = Await.result(db.createRun("name", "dir", "version", "hash", date), Duration.Inf)
    Await.result(db.getRuns(), Duration.Inf) shouldBe Seq(Schema.Run(runId, "name", "dir", "version", "hash", date), Schema.Run(runId2, "name", "dir", "version", "hash", date))

    db.close()
  }

  @Test
  def testSamples(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val date = new Date(System.currentTimeMillis())

    val runId = Await.result(db.createRun("name", "dir", "version", "hash", date), Duration.Inf)
    Await.result(db.getSamples(), Duration.Inf) shouldBe empty
    val sampleId = Await.result(db.createSample("test_sample", runId), Duration.Inf)
    Await.result(db.getSamples(), Duration.Inf) shouldBe Seq(Schema.Sample(sampleId, "test_sample", runId, None))
    Await.result(db.getSampleName(sampleId), Duration.Inf) shouldBe Some("test_sample")
    Await.result(db.getSampleId(runId, "test_sample"), Duration.Inf) shouldBe Some(sampleId)
    Await.result(db.getSampleTags(sampleId), Duration.Inf) shouldBe None

    val sampleId2 = Await.result(db.createSample("test_sample2", runId, Some("""{"test": "test"}""")), Duration.Inf)
    Await.result(db.getSampleTags(sampleId2), Duration.Inf) shouldBe Some(Map("test" -> "test"))
    Await.result(db.getSamples(), Duration.Inf) shouldBe Seq(Schema.Sample(sampleId, "test_sample", runId, None), Schema.Sample(sampleId2, "test_sample2", runId, Some("""{"test": "test"}""")))

    db.close()
  }

  @Test
  def testLibraries(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val date = new Date(System.currentTimeMillis())

    val runId = Await.result(db.createRun("name", "dir", "version", "hash", date), Duration.Inf)
    val sampleId = Await.result(db.createSample("test_sample", runId), Duration.Inf)
    Await.result(db.getLibraries(), Duration.Inf) shouldBe empty
    val libraryId = Await.result(db.createLibrary("test_lib", runId, sampleId), Duration.Inf)

    Await.result(db.getLibraries(), Duration.Inf) shouldBe Seq(Schema.Library(libraryId, "test_lib", runId, sampleId, None))
    Await.result(db.getLibraryName(libraryId), Duration.Inf) shouldBe Some("test_lib")
    Await.result(db.getLibraryId(runId, sampleId, "test_lib"), Duration.Inf) shouldBe Some(libraryId)
    Await.result(db.getLibraryTags(sampleId), Duration.Inf) shouldBe None

    val sampleId2 = Await.result(db.createLibrary("test_lib2", runId, sampleId, Some("""{"test": "test"}""")), Duration.Inf)
    Await.result(db.getLibraryTags(sampleId2), Duration.Inf) shouldBe Some(Map("test" -> "test"))
    Await.result(db.getLibraries(), Duration.Inf) shouldBe Seq(Schema.Library(sampleId, "test_lib", runId, sampleId, None), Schema.Library(sampleId2, "test_lib2", runId, sampleId, Some("""{"test": "test"}""")))

    db.close()
  }

  @Test
  def testPipelines(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val date = new Date(System.currentTimeMillis())

    val runId = Await.result(db.createRun("name", "dir", "version", "hash", date), Duration.Inf)
    Await.result(db.getPipelines(), Duration.Inf) shouldBe empty
    val pipelineId = Await.result(db.createPipeline("test", runId), Duration.Inf)
    Await.result(db.getPipelines(), Duration.Inf) shouldBe Seq(Schema.Pipeline(pipelineId, "test", runId))
    Await.result(db.getPipelineId(runId, "test"), Duration.Inf) shouldBe Some(pipelineId)
    Await.result(db.createPipeline("test", runId), Duration.Inf) shouldBe pipelineId
    Await.result(db.getPipelines(), Duration.Inf) shouldBe Seq(Schema.Pipeline(pipelineId, "test", runId))

    db.close()
  }

  @Test
  def testModules(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val date = new Date(System.currentTimeMillis())

    val runId = Await.result(db.createRun("name", "dir", "version", "hash", date), Duration.Inf)
    val pipelineId = Await.result(db.createPipeline("test", runId), Duration.Inf)
    Await.result(db.getModules(), Duration.Inf) shouldBe empty
    val moduleId = Await.result(db.createModule("test", runId, pipelineId), Duration.Inf)
    Await.result(db.getmoduleId(runId, "test", pipelineId), Duration.Inf) shouldBe Some(moduleId)
    Await.result(db.getModules(), Duration.Inf) shouldBe Seq(Schema.Module(pipelineId, "test", runId, pipelineId))
    Await.result(db.createModule("test", runId, pipelineId), Duration.Inf) shouldBe pipelineId
    Await.result(db.getModules(), Duration.Inf) shouldBe Seq(Schema.Module(pipelineId, "test", runId, pipelineId))

    db.close()
  }

  @Test
  def testSettings(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    Await.result(db.createOrUpdateSetting(0, 0, None, None, None, """{"content": "test" }"""), Duration.Inf)
    Await.result(db.getSetting(0, 0.left, None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test"))
    Await.result(db.createOrUpdateSetting(0, 0, None, None, None, """{"content": "test2" }"""), Duration.Inf)
    Await.result(db.getSetting(0, 0.left, None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test2"))
    db.close()
  }

  @Test
  def testSettingKeys(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    Await.result(db.createOrUpdateSetting(0, 0, Some(0), Some(0), Some(0),
      """
        |{
        |"content": "test",
        |"content2": {
        |  "key": "value"
        |}
        | }""".stripMargin), Duration.Inf)

    db.getSettingKeys(0, 0.left, Some(0.left), Some(0.left), Some(0.left), keyValues = Map()) shouldBe Map()
    db.getSettingKeys(0, 0.left, Some(0.left), Some(0.left), Some(0.left), keyValues = Map("content" -> List("content"))) shouldBe Map("content" -> Some("test"))
    db.getSettingKeys(0, 0.left, Some(0.left), Some(0.left), Some(0.left), keyValues = Map("content" -> List("content2", "key"))) shouldBe Map("content" -> Some("value"))

    db.close()
  }

  @Test
  def testSettingsForSamples(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val sampleId = Await.result(db.createSample("test_sample", 0), Duration.Inf)

    Await.result(db.createOrUpdateSetting(0, 0, Some(0), Some(sampleId), None,
      """
        |{
        |"content": "test",
        |"content2": {
        |  "key": "value"
        |}
        | }""".stripMargin), Duration.Inf)

    db.getSettingsForSamples(0, 0.left, Some(0.left), keyValues = Map()) shouldBe Map(0 -> Map())
    db.getSettingsForSamples(0, 0.left, Some(0.left), keyValues = Map("content" -> List("content"))) shouldBe Map(0 -> Map("content" -> Some("test")))

    db.close()
  }

  @Test
  def testSettingsForLibraries(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val sampleId = Await.result(db.createSample("test_sample", 0), Duration.Inf)
    val libraryId = Await.result(db.createLibrary("test_library", 0, sampleId), Duration.Inf)

    Await.result(db.createOrUpdateSetting(0, 0, Some(0), Some(sampleId), Some(libraryId),
      """
        |{
        |"content": "test",
        |"content2": {
        |  "key": "value"
        |}
        | }""".stripMargin), Duration.Inf)

    db.getSettingsForLibraries(0, 0.left, Some(0.left), keyValues = Map()) shouldBe Map((0, 0) -> Map())
    db.getSettingsForLibraries(0, 0.left, Some(0.left), keyValues = Map("content" -> List("content"))) shouldBe Map((0, 0) -> Map("content" -> Some("test")))

    db.close()
  }

  @Test
  def testStats(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val runId = Await.result(db.createRun("test", "", "", "", new Date(System.currentTimeMillis())), Duration.Inf)
    val pipelineId = Await.result(db.createPipeline("test_pipeline", runId), Duration.Inf)
    val moduleId = Await.result(db.createModule("test_module", runId, pipelineId), Duration.Inf)
    val sampleId = Await.result(db.createSample("test_sample", runId), Duration.Inf)
    val libraryId = Await.result(db.createLibrary("test_library", runId, sampleId), Duration.Inf)

    Await.result(db.getStatsSize(), Duration.Inf) shouldBe 0

    Await.result(db.createOrUpdateStat(runId, pipelineId, None, None, None, """{"content": "test" }"""), Duration.Inf)
    Await.result(db.getStat(runId, pipelineId.left, None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test"))
    Await.result(db.getStatsSize(), Duration.Inf) shouldBe 1
    Await.result(db.createOrUpdateStat(runId, pipelineId, None, None, None, """{"content": "test2" }"""), Duration.Inf)
    Await.result(db.getStat(runId, pipelineId.left, None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test2"))
    Await.result(db.getStatsSize(), Duration.Inf) shouldBe 1

    // Test join queries
    Await.result(db.createOrUpdateStat(runId, pipelineId, Some(moduleId), Some(sampleId), Some(libraryId), """{"content": "test3" }"""), Duration.Inf)
    Await.result(db.getStat(runId, "test_pipeline".right, Some("test_module".right), Some("test_sample".right), Some("test_library".right)), Duration.Inf) shouldBe Some(Map("content" -> "test3"))
    Await.result(db.getStatsSize(), Duration.Inf) shouldBe 2

    db.close()
  }

  @Test
  def testStatKeys(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    Await.result(db.createOrUpdateStat(0, 0, Some(0), Some(0), Some(0),
      """
        |{
        |"content": "test",
        |"content2": {
        |  "key": "value"
        |}
        | }""".stripMargin), Duration.Inf)

    db.getStatKeys(0, 0.left, Some(0.left), Some(0.left), Some(0.left), keyValues = Map()) shouldBe Map()
    db.getStatKeys(0, 0.left, Some(0.left), Some(0.left), Some(0.left), keyValues = Map("content" -> List("content"))) shouldBe Map("content" -> Some("test"))
    db.getStatKeys(0, 0.left, Some(0.left), Some(0.left), Some(0.left), keyValues = Map("content" -> List("content2", "key"))) shouldBe Map("content" -> Some("value"))

    db.close()
  }

  @Test
  def testStatsForSamples(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val sampleId = Await.result(db.createSample("test_sample", 0), Duration.Inf)

    Await.result(db.createOrUpdateStat(0, 0, Some(0), Some(sampleId), None,
      """
        |{
        |"content": "test",
        |"content2": {
        |  "key": "value"
        |}
        | }""".stripMargin), Duration.Inf)

    db.getStatsForSamples(0, 0.left, Some(0.left), keyValues = Map()) shouldBe Map(0 -> Map())
    db.getStatsForSamples(0, 0.left, Some(0.left), keyValues = Map("content" -> List("content"))) shouldBe Map(0 -> Map("content" -> Some("test")))

    db.close()
  }

  @Test
  def testStatsForLibraries(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val sampleId = Await.result(db.createSample("test_sample", 0), Duration.Inf)
    val libraryId = Await.result(db.createLibrary("test_library", 0, sampleId), Duration.Inf)

    Await.result(db.createOrUpdateStat(0, 0, Some(0), Some(sampleId), Some(libraryId),
      """
        |{
        |"content": "test",
        |"content2": {
        |  "key": "value"
        |}
        | }""".stripMargin), Duration.Inf)

    db.getStatsForLibraries(0, 0.left, Some(0.left), keyValues = Map()) shouldBe Map((0, 0) -> Map())
    db.getStatsForLibraries(0, 0.left, Some(0.left), keyValues = Map("content" -> List("content"))) shouldBe Map((0, 0) -> Map("content" -> Some("test")))

    db.close()
  }

  @Test
  def testFiles(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val runId = Await.result(db.createRun("test", "", "", "", new Date(System.currentTimeMillis())), Duration.Inf)
    val pipelineId = Await.result(db.createPipeline("test_pipeline", runId), Duration.Inf)
    val moduleId = Await.result(db.createModule("test_module", runId, pipelineId), Duration.Inf)
    val sampleId = Await.result(db.createSample("test_sample", runId), Duration.Inf)
    val libraryId = Await.result(db.createLibrary("test_library", runId, sampleId), Duration.Inf)

    Await.result(db.createOrUpdateFile(runId, pipelineId, None, None, None, "key", "path", "md5", link = false, 1), Duration.Inf)
    Await.result(db.getFile(runId, pipelineId.left, None, None, None, "key"), Duration.Inf) shouldBe Some(Schema.File(0, 0, None, None, None, "key", "path", "md5", link = false, 1))
    Await.result(db.getFiles(), Duration.Inf) shouldBe Seq(Schema.File(0, 0, None, None, None, "key", "path", "md5", link = false, 1))
    Await.result(db.createOrUpdateFile(runId, pipelineId, None, None, None, "key", "path2", "md5", link = false, 1), Duration.Inf)
    Await.result(db.getFile(runId, pipelineId.left, None, None, None, "key"), Duration.Inf) shouldBe Some(Schema.File(0, 0, None, None, None, "key", "path2", "md5", link = false, 1))
    Await.result(db.getFiles(), Duration.Inf) shouldBe Seq(Schema.File(0, 0, None, None, None, "key", "path2", "md5", link = false, 1))

    // Test join queries
    Await.result(db.createOrUpdateFile(runId, pipelineId, Some(moduleId), Some(sampleId), Some(libraryId), "key", "path3", "md5", link = false, 1), Duration.Inf)
    Await.result(db.getFile(runId, "test_pipeline".right, Some("test_module".right), Some("test_sample".right), Some("test_library".right), "key"), Duration.Inf) shouldBe Some(Schema.File(0, 0, Some(moduleId), Some(sampleId), Some(libraryId), "key", "path3", "md5", link = false, 1))
    Await.result(db.getFiles(), Duration.Inf) shouldBe Seq(Schema.File(0, 0, None, None, None, "key", "path2", "md5", link = false, 1), Schema.File(0, 0, Some(moduleId), Some(sampleId), Some(libraryId), "key", "path3", "md5", link = false, 1))

    db.close()
  }

  @Test
  def testExecutable(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    Await.result(db.createOrUpdateExecutable(0, "name"), Duration.Inf)
    Await.result(db.createOrUpdateExecutable(0, "name", Some("test")), Duration.Inf)
    Await.result(db.getExecutables(Some(0)), Duration.Inf).head shouldBe Schema.Executable(0, "name", Some("test"))
    db.close()
  }

}
