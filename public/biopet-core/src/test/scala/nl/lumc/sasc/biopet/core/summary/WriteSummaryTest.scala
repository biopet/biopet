package nl.lumc.sasc.biopet.core.summary

import java.io.File

import com.google.common.io.Files
import nl.lumc.sasc.biopet.utils.config.{Config, Configurable}
import nl.lumc.sasc.biopet.utils.summary.Summary
import org.broadinstitute.gatk.queue.{QScript, QSettings}
import org.broadinstitute.gatk.queue.function.QFunction
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import WriteSummaryTest._
import org.testng.annotations.Test

import scala.io.Source

/**
  * Created by pjvanthof on 15/01/16.
  */
class WriteSummaryTest extends TestNGSuite with Matchers {

  def testWrongRoot: Unit = {
    intercept[IllegalArgumentException] {
      makeWriter(null)
    }
  }

  @Test
  def testEmpty: Unit = {
    val qscript = makeQscript()
    val writer = makeWriter(qscript)
    writer.freezeFieldValues()

    writer.run()

    val summary = new Summary(writer.out)

    summary.getValue("test") should not be None
    summary.getValue("test", "files") shouldBe Some(Map("pipeline" -> Map()))
    summary.getValue("test", "settings") shouldBe Some(Map())
    summary.getValue("test", "executables") shouldBe Some(Map())

    summary.getValue("meta") should not be None
    summary.getValue("meta", "pipeline_name") shouldBe Some("test")
    summary.getValue("meta", "last_commit_hash") shouldBe Some(nl.lumc.sasc.biopet.LastCommitHash)
    summary.getValue("meta", "pipeline_version") shouldBe Some(nl.lumc.sasc.biopet.Version)
    summary.getValue("meta", "output_dir") shouldBe Some(new File(".").getAbsolutePath)
    summary.getValue("meta", "summary_creation") should not be None
  }
}

object WriteSummaryTest {
  def makeWriter(root: Configurable, c: Map[String, Any] = Map()) = new WriteSummary(root) {
    override def globalConfig = new Config(c)
    override def outputs = Seq()
    override def inputs = Seq()
    qSettings = new QSettings {
      jobName = "test"
      jobTempDir = Files.createTempDir()
      jobTempDir.deleteOnExit()
      jobPriority = Some(1)
    }
    override def absoluteCommandDirectory() {}
  }

  def makeQscript(settings: Map[String, Any] = Map(),
                  files: Map[String, File] = Map(),
                  c: Map[String, Any] = Map()) =
    new SummaryQScript with QScript {
      summaryName = "test"
      outputDir = new File(".").getAbsoluteFile
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