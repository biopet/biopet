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
package nl.lumc.sasc.biopet.core.summary

import java.io.{File, PrintWriter}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.utils.config.{Config, Configurable}
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._
import org.broadinstitute.gatk.queue.function.CommandLineFunction
import org.broadinstitute.gatk.queue.{QScript, QSettings}
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import WriteSummaryTest._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.{NoLibrary, NoSample}
import org.apache.commons.io.FileUtils
import org.testng.annotations.{AfterClass, Test}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex

/**
  * Created by pjvanthof on 15/01/16.
  */
class WriteSummaryTest extends TestNGSuite with Matchers {

  @Test
  def testCreateFile(): Unit = {
    val dbFile = File.createTempFile("summary.", ".db")
    dbFile.deleteOnExit()
    val db = SummaryDb.openSqliteSummary(dbFile)
    db.createTables()

    val outputDir = new File("/tmp")
    Await.ready(WriteSummary.createFile(db,
                                        0,
                                        0,
                                        Some(0),
                                        Some(0),
                                        Some(0),
                                        "test",
                                        new File(outputDir, "test.tsv"),
                                        outputDir),
                Duration.Inf)
    val file = Await.result(db.getFile(0, 0, 0, 0, 0, "test"), Duration.Inf)
    file.map(_.path) shouldBe Some("./test.tsv")

    Await.ready(WriteSummary.createFile(db,
                                        0,
                                        0,
                                        Some(0),
                                        Some(0),
                                        Some(0),
                                        "test",
                                        new File("/tmp2/test.tsv"),
                                        outputDir),
                Duration.Inf)
    val file2 = Await.result(db.getFile(0, 0, 0, 0, 0, "test"), Duration.Inf)
    file2.map(_.path) shouldBe Some("/tmp2/test.tsv")

    db.close()
  }

  @Test
  def testWrongRoot(): Unit = {
    intercept[IllegalArgumentException] {
      makeWriter(null)
    }
  }

  private var dirs: List[File] = Nil

  /** This is a basic summary test, no matter the content this should always be true */
  def basicSummaryTest(summary: SummaryDb,
                       runId: Int,
                       dir: File,
                       sampleId: Option[String] = None,
                       libId: Option[String] = None): Unit = {
    val run = Await.result(summary.getRuns(runId = Some(runId)), Duration.Inf).head
    run.commitHash shouldBe nl.lumc.sasc.biopet.LastCommitHash
    run.version shouldBe nl.lumc.sasc.biopet.Version
    run.outputDir shouldBe dir.getAbsolutePath

  }

  def createFakeCheckSum(file: File): Unit = {
    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    writer.println("checksum  file")
    writer.close()
    file.deleteOnExit()
  }

  @Test
  def testEmpty(): Unit = {
    val dir = Files.createTempDir()
    dirs :+= dir
    val qscript = makeQscript(name = "test", dir = dir)
    val writer = makeWriter(qscript)
    writer.freezeFieldValues()
    writer.deps shouldBe empty
    writer.run()

    val summary = SummaryDb.openSqliteSummary(qscript.summaryDbFile)
    basicSummaryTest(summary, qscript.summaryRunId, dir)
  }

  @Test
  def testSingleJob(): Unit = {
    val dir = Files.createTempDir()
    dirs :+= dir

    val qscript = makeQscript("test", dir = dir)
    val writer = makeWriter(qscript)
    val summarizable =
      makeSummarizable(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.run()

    val summary = SummaryDb.openSqliteSummary(qscript.summaryDbFile)
    basicSummaryTest(summary, qscript.summaryRunId, dir)
    summary.getStatKeys(qscript.summaryRunId,
                        "test",
                        "tool_1",
                        keyValues = Map("key" -> List("key"))) shouldBe Map("key" -> Some("value"))
    Await
      .result(summary.getFile(qscript.summaryRunId, "test", "tool_1", key = "file_1"),
              Duration.Inf)
      .map(_.md5) shouldBe Some("checksum")
  }

  @Test
  def testSingleJavaJob(): Unit = {
    val dir = Files.createTempDir()
    dirs :+= dir

    val qscript = makeQscript("test", dir = dir)
    val writer = makeWriter(qscript)
    val summarizable =
      makeJavaCommand(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.add(summarizable)
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.run()

    val summary = SummaryDb.openSqliteSummary(qscript.summaryDbFile)
    basicSummaryTest(summary, qscript.summaryRunId, dir)
    summary.getStatKeys(qscript.summaryRunId,
                        "test",
                        "tool_1",
                        keyValues = Map("key" -> List("key"))) shouldBe Map("key" -> Some("value"))
    Await
      .result(summary.getFile(qscript.summaryRunId,
                              "test",
                              "tool_1",
                              NoSample,
                              NoLibrary,
                              key = "file_1"),
              Duration.Inf)
      .map(_.md5) shouldBe Some("checksum")
  }

  @Test
  def testVersion(): Unit = {
    val dir = Files.createTempDir()
    dirs :+= dir

    val qscript = makeQscript("test", dir = dir)
    val writer = makeWriter(qscript)
    val summarizable = makeVersionSummarizable(files = Map("file_1" -> new File("bla")),
                                               stats = Map("key" -> "value"))
    qscript.add(summarizable)
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.run()

    val summary = SummaryDb.openSqliteSummary(qscript.summaryDbFile)
    basicSummaryTest(summary, qscript.summaryRunId, dir)
    summary.getStatKeys(qscript.summaryRunId,
                        "test",
                        "tool_1",
                        keyValues = Map("key" -> List("key"))) shouldBe Map("key" -> Some("value"))
    Await
      .result(summary.getFile(qscript.summaryRunId, "test", "tool_1", key = "file_1"),
              Duration.Inf)
      .map(_.md5) shouldBe Some("checksum")
  }

  @Test
  def testSampleLibrary(): Unit = {
    val dir = Files.createTempDir()
    dirs :+= dir

    val qscript =
      makeSampleLibraryQscript("test", s = Some("sampleName"), l = Some("libName"), dir = dir)
    val writer = makeWriter(qscript)
    val summarizable =
      makeSummarizable(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.deps shouldBe empty
    writer.run()

    val summary = SummaryDb.openSqliteSummary(qscript.summaryDbFile)
    basicSummaryTest(summary, qscript.summaryRunId, dir)
    summary.getStatKeys(qscript.summaryRunId,
                        "test",
                        "tool_1",
                        "sampleName",
                        "libName",
                        keyValues = Map("key" -> List("key"))) shouldBe Map("key" -> Some("value"))
    Await
      .result(summary.getFile(qscript.summaryRunId,
                              "test",
                              "tool_1",
                              "sampleName",
                              "libName",
                              key = "file_1"),
              Duration.Inf)
      .map(_.md5) shouldBe Some("checksum")
  }

  @Test
  def testSample(): Unit = {
    val dir = Files.createTempDir()
    dirs :+= dir

    val qscript = makeSampleLibraryQscript("test", s = Some("sampleName"), dir = dir)
    val writer = makeWriter(qscript)
    val summarizable =
      makeSummarizable(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.init()
    writer.freezeFieldValues()
    writer.deps shouldBe empty
    writer.run()

    val summary = SummaryDb.openSqliteSummary(qscript.summaryDbFile)
    basicSummaryTest(summary, qscript.summaryRunId, dir)
  }

  @Test
  def testMultisampleQscript(): Unit = {
    val dir = Files.createTempDir()
    dirs :+= dir

    val qscript = makeMultisampleQscript("test", multisampleConfig, dir = dir)
    val writer = makeWriter(qscript)
    val summarizable =
      makeSummarizable(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.deps shouldBe empty
    writer.run()

    val summary = SummaryDb.openSqliteSummary(qscript.summaryDbFile)
    basicSummaryTest(summary, qscript.summaryRunId, dir)
    summary.getStatKeys(qscript.summaryRunId,
                        "test",
                        "tool_1",
                        keyValues = Map("key" -> List("key"))) shouldBe Map("key" -> Some("value"))
    Await
      .result(summary.getFile(qscript.summaryRunId, "test", "tool_1", key = "file_1"),
              Duration.Inf)
      .map(_.md5) shouldBe Some("checksum")
  }

  @AfterClass
  def removeDirs(): Unit = {
    dirs.foreach(FileUtils.deleteDirectory)
  }
}

object WriteSummaryTest {
  def makeWriter(root: SummaryQScript, c: Map[String, Any] = Map()) = new WriteSummary(root) {
    override def globalConfig = new Config(c + ("exe" -> "test"))
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

  def makeQscript(name: String,
                  settings: Map[String, Any] = Map(),
                  files: Map[String, File] = Map(),
                  c: Map[String, Any] = Map(),
                  dir: File) =
    new SummaryQScript with QScript {
      summaryName = name
      outputDir = dir
      override def globalConfig = new Config(c)
      def summarySettings: Map[String, Any] = settings
      def summaryFiles: Map[String, File] = files
      val tempFile: File = File.createTempFile("summary", ".json")
      tempFile.deleteOnExit()
      def summaryFile: File = tempFile
      def init(): Unit = {}
      def biopetScript(): Unit = {}
      def parent: Configurable = null
    }

  def makeSampleLibraryQscript(name: String,
                               settings: Map[String, Any] = Map(),
                               files: Map[String, File] = Map(),
                               c: Map[String, Any] = Map(),
                               s: Option[String] = None,
                               l: Option[String] = None,
                               dir: File) =
    new SummaryQScript with QScript with SampleLibraryTag {
      sampleId = s
      libId = l
      summaryName = "test"
      outputDir = dir
      override def globalConfig = new Config(c + ("exe" -> "test"))
      def summarySettings: Map[String, Any] = settings
      def summaryFiles: Map[String, File] = files
      val tempFile: File = File.createTempFile("summary", ".json")
      tempFile.deleteOnExit()
      def summaryFile: File = tempFile
      def init(): Unit = {}
      def biopetScript(): Unit = {}
      def parent: Configurable = null
    }

  def makeMultisampleQscript(name: String,
                             c: Map[String, Any],
                             settings: Map[String, Any] = Map(),
                             files: Map[String, File] = Map(),
                             dir: File) =
    new MultiSampleQScript with QScript {
      summaryName = "test"
      outputDir = dir
      override def globalConfig = new Config(c + ("exe" -> "test"))
      def summarySettings: Map[String, Any] = settings
      def summaryFiles: Map[String, File] = files
      val tempFile: File = File.createTempFile("summary", ".json")
      tempFile.deleteOnExit()
      def summaryFile: File = tempFile
      def init(): Unit = {}
      def biopetScript(): Unit = {}
      def parent: Configurable = null

      class Sample(id: String) extends AbstractSample(id) {
        class Library(id: String) extends AbstractLibrary(id) {
          protected def addJobs(): Unit = {}
          def summaryFiles: Map[String, File] = files
          def summaryStats: Any = Map()
        }

        def makeLibrary(id: String): Library = new Library(id)
        protected def addJobs(): Unit = {}
        def summaryFiles: Map[String, File] = files
        def summaryStats: Any = Map()
      }

      def makeSample(id: String): Sample = new Sample(id)

      def addMultiSampleJobs(): Unit = {}
    }

  val multisampleConfig = Map(
    "samples" -> Map("sampleName" -> Map("libraries" -> Map("libName" -> Map()))))

  def makeSummarizable(files: Map[String, File] = Map(), stats: Map[String, Any] = Map()) =
    new Summarizable {
      def summaryFiles: Map[String, File] = files
      def summaryStats: Any = stats
    }

  def makeJavaCommand(files: Map[String, File] = Map(),
                      stats: Map[String, Any] = Map(),
                      c: Map[String, Any] = Map()) =
    new BiopetJavaCommandLineFunction with Summarizable with Version {
      override def globalConfig = new Config(c)
      override def configNamespace = "java_command"
      def parent: Configurable = null
      def summaryStats: Map[String, Any] = stats
      def summaryFiles: Map[String, File] = files

      def versionCommand: String = "echo test version"
      def versionRegex: List[Regex] = """(.*)""".r :: Nil
      override def getVersion = Some("test version")

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

  def makeVersionSummarizable(files: Map[String, File] = Map(),
                              stats: Map[String, Any] = Map(),
                              c: Map[String, Any] = Map()) =
    new CommandLineFunction with Configurable with Summarizable with Version {
      override def globalConfig = new Config(c)
      override def configNamespace = "version_command"
      def parent: Configurable = null

      def summaryFiles: Map[String, File] = files
      def summaryStats: Any = stats

      def versionCommand: String = "echo test version"
      def versionRegex: List[Regex] = """(.*)""".r :: Nil
      override def getVersion = Some("test version")

      def commandLine: String = ""

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

}
