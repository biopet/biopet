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

import java.io.{ PrintWriter, File }

import com.google.common.io.Files
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.utils.config.{ Config, Configurable }
import nl.lumc.sasc.biopet.utils.summary.Summary
import org.broadinstitute.gatk.queue.function.CommandLineFunction
import org.broadinstitute.gatk.queue.{ QScript, QSettings }
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import WriteSummaryTest._
import org.testng.annotations.Test

import scala.util.matching.Regex

/**
 * Created by pjvanthof on 15/01/16.
 */
class WriteSummaryTest extends TestNGSuite with Matchers {

  @Test
  def testWrongRoot(): Unit = {
    intercept[IllegalArgumentException] {
      makeWriter(null)
    }
  }

  /** This is a basic summary test, no matter the content this should always be true */
  def basicSummaryTest(summary: Summary,
                       name: String,
                       sampleId: Option[String] = None,
                       libId: Option[String] = None): Unit = {
    summary.getValue(sampleId, libId, name) should not be None
    summary.getValue(sampleId, libId, name, "files", "pipeline").get shouldBe a[Map[_, _]]
    summary.getValue(sampleId, libId, name, "settings").get shouldBe a[Map[_, _]]
    summary.getValue(sampleId, libId, name, "executables").get shouldBe a[Map[_, _]]

    summary.getValue("meta") should not be None
    summary.getValue("meta", "pipeline_name") shouldBe Some(name)
    summary.getValue("meta", "last_commit_hash") shouldBe Some(nl.lumc.sasc.biopet.LastCommitHash)
    summary.getValue("meta", "pipeline_version") shouldBe Some(nl.lumc.sasc.biopet.Version)
    summary.getValue("meta", "output_dir") shouldBe Some(new File(".").getAbsolutePath)
    summary.getValue("meta", "summary_creation") should not be None
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
    val qscript = makeQscript(name = "test")
    val writer = makeWriter(qscript)
    writer.freezeFieldValues()
    writer.deps shouldBe empty
    writer.run()

    val summary = new Summary(writer.out)
    basicSummaryTest(summary, "test")
  }

  @Test
  def testMergeQscript(): Unit = {
    val qscript = makeQscript(name = "test")
    val qscript2 = makeQscript(name = "test2")
    qscript.addSummaryQScript(qscript2)
    val summaryWriter = new PrintWriter(qscript2.summaryFile)
    summaryWriter.println("""{ "test2": "value" }""")
    summaryWriter.close()
    val writer = makeWriter(qscript)
    writer.freezeFieldValues()
    writer.run()

    val summary = new Summary(writer.out)
    basicSummaryTest(summary, "test")
    summary.getValue("test2") shouldBe Some("value")
  }

  @Test
  def testSingleJob(): Unit = {
    val qscript = makeQscript("test")
    val writer = makeWriter(qscript)
    val summarizable = makeSummarizable(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.run()

    val summary = new Summary(writer.out)
    basicSummaryTest(summary, "test")
    summary.getValue("test", "stats", "tool_1", "key") shouldBe Some("value")
    summary.getValue("test", "files", "tool_1", "file_1", "md5") shouldBe Some("checksum")
  }

  @Test
  def testSingleJavaJob(): Unit = {
    val qscript = makeQscript("test")
    val writer = makeWriter(qscript)
    val summarizable = makeJavaCommand(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.add(summarizable)
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.run()

    val summary = new Summary(writer.out)
    basicSummaryTest(summary, "test")
    summary.getValue("test", "stats", "tool_1", "key") shouldBe Some("value")
    summary.getValue("test", "files", "tool_1", "file_1", "md5") shouldBe Some("checksum")
    summary.getValue("test", "executables", "java_command", "version") shouldBe Some("test version")
  }

  @Test
  def testVersion(): Unit = {
    val qscript = makeQscript("test")
    val writer = makeWriter(qscript)
    val summarizable = makeVersionSummarizable(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.add(summarizable)
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.run()

    val summary = new Summary(writer.out)
    basicSummaryTest(summary, "test")
    summary.getValue("test", "stats", "tool_1", "key") shouldBe Some("value")
    summary.getValue("test", "files", "tool_1", "file_1", "md5") shouldBe Some("checksum")
    summary.getValue("test", "executables", "version_command", "version") shouldBe Some("test version")
  }

  @Test
  def testSampleLibrary(): Unit = {
    val qscript = makeSampleLibraryQscript("test", s = Some("sampleName"), l = Some("libName"))
    val writer = makeWriter(qscript)
    val summarizable = makeSummarizable(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.deps shouldBe empty
    writer.run()

    val summary = new Summary(writer.out)
    basicSummaryTest(summary, "test", sampleId = Some("sampleName"), libId = Some("libName"))
    summary.getValue(Some("sampleName"), Some("libName"), "test", "stats", "tool_1", "key") shouldBe Some("value")
    summary.getValue(Some("sampleName"), Some("libName"), "test", "files", "tool_1", "file_1", "md5") shouldBe Some("checksum")
  }

  @Test
  def testSample(): Unit = {
    val qscript = makeSampleLibraryQscript("test", s = Some("sampleName"))
    val writer = makeWriter(qscript)
    val summarizable = makeSummarizable(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.deps shouldBe empty
    writer.run()

    val summary = new Summary(writer.out)
    basicSummaryTest(summary, "test", sampleId = Some("sampleName"), libId = None)
    summary.getValue(Some("sampleName"), None, "test", "stats", "tool_1", "key") shouldBe Some("value")
    summary.getValue(Some("sampleName"), None, "test", "files", "tool_1", "file_1", "md5") shouldBe Some("checksum")
  }

  @Test
  def testMultisampleQscript(): Unit = {
    val qscript = makeMultisampleQscript("test", multisampleConfig)
    val writer = makeWriter(qscript)
    val summarizable = makeSummarizable(files = Map("file_1" -> new File("bla")), stats = Map("key" -> "value"))
    qscript.addSummarizable(summarizable, "tool_1")
    qscript.addSummaryJobs()
    createFakeCheckSum(SummaryQScript.md5sumCache(new File("bla")))
    writer.freezeFieldValues()
    writer.deps shouldBe empty
    writer.run()

    val summary = new Summary(writer.out)
    basicSummaryTest(summary, "test")
    summary.getValue("test", "stats", "tool_1", "key") shouldBe Some("value")
    summary.getValue("test", "files", "tool_1", "file_1", "md5") shouldBe Some("checksum")

    summary.getValue(Some("sampleName"), Some("libName"), "test") should not be None
  }

}

object WriteSummaryTest {
  def makeWriter(root: Configurable, c: Map[String, Any] = Map()) = new WriteSummary(root) {
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
                  c: Map[String, Any] = Map()) =
    new SummaryQScript with QScript {
      summaryName = name
      outputDir = new File(".").getAbsoluteFile
      override def globalConfig = new Config(c)
      def summarySettings: Map[String, Any] = settings
      def summaryFiles: Map[String, File] = files
      val tempFile = File.createTempFile("summary", ".json")
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
                               l: Option[String] = None) =
    new SummaryQScript with QScript with SampleLibraryTag {
      sampleId = s
      libId = l
      summaryName = "test"
      outputDir = new File(".").getAbsoluteFile
      override def globalConfig = new Config(c + ("exe" -> "test"))
      def summarySettings: Map[String, Any] = settings
      def summaryFiles: Map[String, File] = files
      val tempFile = File.createTempFile("summary", ".json")
      tempFile.deleteOnExit()
      def summaryFile: File = tempFile
      def init(): Unit = {}
      def biopetScript(): Unit = {}
      def parent: Configurable = null
    }

  def makeMultisampleQscript(name: String,
                             c: Map[String, Any],
                             settings: Map[String, Any] = Map(),
                             files: Map[String, File] = Map()) =
    new MultiSampleQScript with QScript {
      summaryName = "test"
      outputDir = new File(".").getAbsoluteFile
      override def globalConfig = new Config(c + ("exe" -> "test"))
      def summarySettings: Map[String, Any] = settings
      def summaryFiles: Map[String, File] = files
      val tempFile = File.createTempFile("summary", ".json")
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

  val multisampleConfig = Map("samples" -> Map("sampleName" -> Map("libraries" -> Map("libName" -> Map()))))

  def makeSummarizable(files: Map[String, File] = Map(), stats: Map[String, Any] = Map()) = new Summarizable {
    def summaryFiles: Map[String, File] = files
    def summaryStats: Any = stats
  }

  def makeJavaCommand(files: Map[String, File] = Map(),
                      stats: Map[String, Any] = Map(),
                      c: Map[String, Any] = Map()) = new BiopetJavaCommandLineFunction with Summarizable with Version {
    override def globalConfig = new Config(c)
    override def configNamespace = "java_command"
    def parent: Configurable = null
    def summaryStats: Map[String, Any] = stats
    def summaryFiles: Map[String, File] = files

    def versionCommand: String = "echo test version"
    def versionRegex: Regex = """(.*)""".r
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
      def versionRegex: Regex = """(.*)""".r
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