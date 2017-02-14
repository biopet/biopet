package nl.lumc.sasc.biopet.utils.summary.db

import java.io.File

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
    Await.result(db.getSetting(0,0, None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test"))
    Await.result(db.createOrUpdateSetting(0, 0, None, None, None, """{"content": "test2" }"""), Duration.Inf)
    Await.result(db.getSetting(0,0, None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test2"))

  }

  @Test
  def testStats: Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    Await.result(db.createOrUpdateStat(0, 0, None, None, None, """{"content": "test" }"""), Duration.Inf)
    Await.result(db.getStat(0, 0, None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test"))
    Await.result(db.createOrUpdateStat(0, 0, None, None, None, """{"content": "test2" }"""), Duration.Inf)
    Await.result(db.getStat(0, 0, None, None, None), Duration.Inf) shouldBe Some(Map("content" -> "test2"))
  }

}
