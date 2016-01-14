package nl.lumc.sasc.biopet.core.summary

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetQScript.InputFile
import nl.lumc.sasc.biopet.core.extensions.Md5sum
import nl.lumc.sasc.biopet.utils.config.{Config, Configurable}
import org.broadinstitute.gatk.queue.{QScript, QSettings}
import org.broadinstitute.gatk.queue.function.QFunction
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import SummaryQScriptTest._

/**
  * Created by pjvanthof on 14/01/16.
  */
class SummaryQScriptTest extends TestNGSuite with Matchers {
  @Test
  def testNoJobs: Unit = {
    SummaryQScript.md5sumCache.clear()
    val script = makeQscript()
    script.addSummaryJobs()
    SummaryQScript.md5sumCache shouldBe empty
  }

  @Test
  def testFiles: Unit = {
    SummaryQScript.md5sumCache.clear()
    val file = new File(s".${File.separator}bla")
    val script = makeQscript(files = Map("file" -> file))
    script.addSummaryJobs()
    SummaryQScript.md5sumCache should not be empty
    SummaryQScript.md5sumCache.toMap shouldBe Map(
      new File(s".${File.separator}bla") -> new File(s".${File.separator}bla.md5"))
    script.functions.size shouldBe 2
    assert(script.functions
      .filter(_.isInstanceOf[Md5sum])
      .map(_.asInstanceOf[Md5sum])
      .exists(_.cmdLine.contains(" || ")))
  }

  @Test
  def testDuplicateFiles: Unit = {
    SummaryQScript.md5sumCache.clear()
    val file = new File(s".${File.separator}bla")
    val script = makeQscript(files = Map("file" -> file, "file2" -> file))
    script.addSummaryJobs()
    SummaryQScript.md5sumCache should not be empty
    SummaryQScript.md5sumCache.toMap shouldBe Map(
      new File(s".${File.separator}bla") -> new File(s".${File.separator}bla.md5"))
    script.functions.size shouldBe 2
    assert(script.functions
      .filter(_.isInstanceOf[Md5sum])
      .map(_.asInstanceOf[Md5sum])
      .exists(_.cmdLine.contains(" || ")))
  }

  @Test
  def testAddSummarizable: Unit = {
    SummaryQScript.md5sumCache.clear()
    val file = new File(s".${File.separator}bla")
    val script = makeQscript()
    script.addSummarizable(makeSummarizable(files = Map("file" -> file, "file2" -> file)), "test")
    script.summarizables.size shouldBe 1
    script.addSummaryJobs()
    SummaryQScript.md5sumCache should not be empty
    SummaryQScript.md5sumCache.toMap shouldBe Map(
      new File(s".${File.separator}bla") -> new File(s".${File.separator}bla.md5"))
    script.functions.size shouldBe 2
    assert(script.functions
      .filter(_.isInstanceOf[Md5sum])
      .map(_.asInstanceOf[Md5sum])
      .exists(_.cmdLine.contains(" || ")))
  }

  @Test
  def testInputFile: Unit = {
    SummaryQScript.md5sumCache.clear()
    val file = new File(s".${File.separator}bla")
    val script = makeQscript()
    script.addSummarizable(makeSummarizable(files = Map("file" -> file, "file2" -> file)), "test")
    script.summarizables.size shouldBe 1
    script.inputFiles :+= InputFile(file, Some("md5sum"))
    script.inputFiles :+= InputFile(file, None)
    script.addSummaryJobs()
    SummaryQScript.md5sumCache should not be empty
    SummaryQScript.md5sumCache.toMap shouldBe Map(
      new File(s".${File.separator}bla") -> new File(s".${File.separator}bla.md5"))
    script.functions.size shouldBe 3
    assert(script.functions
      .filter(_.isInstanceOf[Md5sum])
      .map(_.asInstanceOf[Md5sum])
      .exists(_.cmdLine.contains(" || ")))
  }

  @Test
  def testAddQscript: Unit = {
    SummaryQScript.md5sumCache.clear()
    val script = makeQscript()
    script.addSummaryQScript(script)
    script.summaryQScripts.head shouldBe script
  }
}

object SummaryQScriptTest {
  def makeQscript(settings: Map[String, Any] = Map(),
                  files: Map[String, File] = Map(),
                  c: Map[String, Any] = Map()) =
    new SummaryQScript with QScript {
      outputDir = new File(".")
      override def globalConfig = new Config(c)
      def summarySettings: Map[String, Any] = settings
      def summaryFiles: Map[String, File] = files
      val tempFile = File.createTempFile("summary", ".json")
      tempFile.deleteOnExit()
      def summaryFile: File = tempFile
      def init(): Unit = ???
      def biopetScript(): Unit = ???
      def root: Configurable = null
    }

  def makeSummarizable(files: Map[String, File] = Map(), stats: Map[String, Any] = Map()) = new Summarizable {
    def summaryFiles: Map[String, File] = files
    def summaryStats: Any = stats
  }
}