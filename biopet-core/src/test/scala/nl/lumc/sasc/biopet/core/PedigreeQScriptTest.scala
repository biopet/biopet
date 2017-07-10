package nl.lumc.sasc.biopet.core

import java.io.File
import java.nio.file.Paths

import nl.lumc.sasc.biopet.core.MultiSampleQScript.Gender
import nl.lumc.sasc.biopet.core.extensions.Md5sum
import nl.lumc.sasc.biopet.utils.config.Config
import nl.lumc.sasc.biopet.utils.{ConfigUtils, Logging}
import org.broadinstitute.gatk.queue.{QScript, QSettings}
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.language.reflectiveCalls
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Created by Sander Bollen on 11-4-17.
  */
class PedigreeQScriptTest extends TestNGSuite with Matchers {
  import PedigreeQScriptTest._

  @Test
  def testConfigPedigree(): Unit = {
    val script = PedigreeQScriptTest(sample1 :: sample2 :: sample3 :: Nil)
    script.init()
    script.biopetScript()

    script.pedSamples.size shouldBe 3
    script.pedSamples.map(_.individualId).contains("sample1") shouldBe true
    script.pedSamples.map(_.individualId).contains("sample2") shouldBe true
    script.pedSamples.map(_.individualId).contains("sample3") shouldBe true
  }

  @Test
  def testGenderCorrect(): Unit = {
    val script = PedigreeQScriptTest(sample2 :: Nil)
    script.init()
    script.biopetScript()

    script.pedSamples.size shouldBe 1
    script.pedSamples.head.gender shouldEqual Gender.Male

    val script2 = PedigreeQScriptTest(sample3 :: Nil)
    script2.init()
    script2.biopetScript()

    script2.pedSamples.size shouldBe 1
    script2.pedSamples.head.gender shouldEqual Gender.Female
  }

  @Test
  def testIsSingle(): Unit = {
    val script = PedigreeQScriptTest(sample2 :: Nil)
    script.init()
    script.biopetScript()

    script.isSingle shouldBe true
  }

  @Test
  def testPedParsing(): Unit = {
    val script = PedigreeQScriptTest(trioPed :: Nil)
    script.init()
    script.biopetScript()

    script.pedSamples.size shouldBe 3
    script.pedSamples.map(_.individualId).contains("sample4") shouldBe true
    script.pedSamples.map(_.individualId).contains("sample5") shouldBe true
    script.pedSamples.map(_.individualId).contains("sample6") shouldBe true
  }

  @Test
  def testIsMother() = {
    val script = PedigreeQScriptTest(trioPed :: Nil)
    script.init()
    script.biopetScript()

    script.isMother(script.pedSamples.filter(_.individualId == "sample6").head) shouldBe true
  }

  @Test
  def testIsFather() = {
    val script = PedigreeQScriptTest(trioPed :: Nil)
    script.init()
    script.biopetScript()

    script.isFather(script.pedSamples.filter(_.individualId == "sample5").head) shouldBe true
  }

  @Test
  def testIsTrio() = {
    val script = PedigreeQScriptTest(trioPed :: Nil)
    script.init()
    script.biopetScript()

    script.isTrio shouldBe true

    val script2 = PedigreeQScriptTest(sample1 :: sample2 :: sample3 :: Nil)
    script2.init()
    script2.biopetScript()

    script2.isTrio shouldBe false
  }

  @Test
  def testConcatenation() = {
    val script = PedigreeQScriptTest(sample1 :: sample2 :: sample3 :: trioPed :: Nil)
    script.init()
    script.biopetScript()

    script.pedSamples.size shouldBe 6
    script.pedSamples.map(_.individualId).contains("sample1") shouldBe true
    script.pedSamples.map(_.individualId).contains("sample2") shouldBe true
    script.pedSamples.map(_.individualId).contains("sample3") shouldBe true
    script.pedSamples.map(_.individualId).contains("sample4") shouldBe true
    script.pedSamples.map(_.individualId).contains("sample5") shouldBe true
    script.pedSamples.map(_.individualId).contains("sample6") shouldBe true
  }

  @Test
  def testWritePedFile(): Unit = {
    val script = PedigreeQScriptTest(sample1 :: sample2 :: sample3 :: trioPed :: Nil)
    script.init()
    script.biopetScript()

    val tmpFile = File.createTempFile("test", ".ped")
    tmpFile.deleteOnExit()
    script.writeToPedFile(tmpFile)

    val expectedLines = Source.fromFile(resourcePath("/full.ped")).getLines().toList.sorted
    val writtenLines = Source.fromFile(tmpFile).getLines().toList.sorted

    writtenLines shouldEqual expectedLines
  }

}

object PedigreeQScriptTest {

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val sample1 = Map(
    "samples" ->
      Map(
        "sample1" ->
          Map(
            "tags" ->
              Map("gender" -> "female",
                  "father" -> "sample2",
                  "mother" -> "sample3",
                  "family" -> "fam01"))))

  val sample2 = Map(
    "samples" ->
      Map(
        "sample2" ->
          Map("tags" ->
            Map("gender" -> "male", "family" -> "fam01"))))

  val sample3 = Map(
    "samples" ->
      Map(
        "sample3" ->
          Map("tags" ->
            Map("gender" -> "female", "family" -> "fam01"))))

  val trioPed = Map("ped_file" -> resourcePath("/trio.ped"))

  def apply(configs: List[Map[String, Any]], only: List[String] = Nil) = {
    new QScript with PedigreeQscript { qscript =>

      qSettings = new QSettings()
      qSettings.runName = "test"

      override val onlySamples = only

      var buffer = new ListBuffer[String]()

      override def globalConfig =
        new Config(
          configs
            .foldLeft(Map[String, Any]()) { case (a, b) => ConfigUtils.mergeMaps(a, b) })

      val parent = null

      def getLastLogMessage: String = {
        Logging.errors.toList.last.getMessage
      }

      class Sample(id: String) extends AbstractSample(id) {
        class Library(id: String) extends AbstractLibrary(id) {

          /** Function that add library jobs */
          protected def addJobs(): Unit = {
            buffer += config("test")
          }

          /** Must return files to store into summary */
          def summaryFiles: Map[String, File] = Map()

          /** Must returns stats to store into summary */
          def summaryStats = Map()
        }

        /**
          * Factory method for Library class
          * @param id SampleId
          * @return Sample class
          */
        def makeLibrary(id: String): Library = new Library(id)

        /** Function to add sample jobs */
        protected def addJobs(): Unit = {
          buffer += s"$sampleId"
          addPerLibJobs()
          add(new Md5sum(qscript))
        }

        /** Must return files to store into summary */
        def summaryFiles: Map[String, File] = Map()

        /** Must returns stats to store into summary */
        def summaryStats = Map()
      }

      /**
        * Method where the multisample jobs should be added, this will be executed only when running the -sample argument is not given.
        */
      def addMultiSampleJobs(): Unit = {
        add(new Md5sum(qscript))
      }

      /**
        * Factory method for Sample class
        * @param id SampleId
        * @return Sample class
        */
      def makeSample(id: String): Sample = new Sample(id)

      /** Must return a map with used settings for this pipeline */
      def summarySettings: Map[String, Any] = Map()

      /** File to put in the summary for thie pipeline */
      def summaryFiles: Map[String, File] = Map()

      /** Name of summary output file */
      def summaryFile: File = new File("./summary.json")

      /** Init for pipeline */
      def init(): Unit = {}

      /** Pipeline itself */
      def biopetScript(): Unit = {
        addSamplesJobs()
        addSummaryJobs()
      }
    }
  }
}
