package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.core.MultiSampleQScript.Gender
import nl.lumc.sasc.biopet.core.extensions.Md5sum
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QScript
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.collection.mutable.ListBuffer

/**
  * Created by pjvan_thof on 12/29/15.
  */
class MultiSampleQScriptTest extends TestNGSuite with Matchers {
  import MultiSampleQScriptTest._

  @Test
  def testDefault: Unit = {

    val script = MultiSampleQScriptTest(sample1 :: sample2 :: sample3 :: Nil)
    script.outputDir = new File("./").getAbsoluteFile
    script.init()
    script.biopetScript()

    script.functions.size shouldBe 4

    script.samples.foreach { case (sampleId, sample) =>
      sample.gender shouldBe Gender.Unknown
      sample.father shouldBe None
      sample.mother shouldBe None
      sample.summaryFiles shouldBe Map()
      sample.summaryStats shouldBe Map()
      sample.summarySettings shouldBe Map()
      sample.sampleDir shouldBe new File(script.outputDir, "samples" + File.separator + sampleId)
      sample.createFile("bla.txt") shouldBe new File(sample.sampleDir, s"$sampleId.bla.txt")

      sample.libraries.foreach { case (libId, library) =>
        library.libDir shouldBe new File(sample.sampleDir, s"lib_$libId")
        library.createFile("bla.txt") shouldBe new File(library.libDir, s"$sampleId-$libId.bla.txt")
        library.summaryFiles shouldBe Map()
        library.summaryStats shouldBe Map()
        library.summarySettings shouldBe Map()
      }
    }
  }

  @Test
  def testTrio: Unit = {
    val script = MultiSampleQScriptTest(child :: father :: mother :: Nil)
    script.init()
    script.biopetScript()

    script.functions.size shouldBe 4

    script.samples("child").gender shouldBe Gender.Male
    script.samples("father").gender shouldBe Gender.Male
    script.samples("mother").gender shouldBe Gender.Female
    script.samples("child").father shouldBe Some("father")
    script.samples("child").mother shouldBe Some("mother")
  }

  @Test
  def testGroups: Unit = {
    val script = MultiSampleQScriptTest(sample1 :: sample2 :: sample3 :: Nil)
    script.init()
    script.biopetScript()

    script.functions.size shouldBe 4

    script.samples("sample1").sampleGroups shouldBe List("1")
    script.samples("sample1").libraries("lib1").libGroups shouldBe List("1")
    script.samples("sample2").sampleGroups shouldBe List("2")
    script.samples("sample2").libraries("lib1").libGroups shouldBe List("3")

    script.samples("sample3").sampleGroups shouldBe Nil
  }

  @Test
  def testOnlySamples: Unit = {
    val script = MultiSampleQScriptTest(sample1 :: sample2 :: sample3 :: Nil, List("sample1"))
    script.init()
    script.biopetScript()

    script.functions.size shouldBe 1
  }
}

object MultiSampleQScriptTest {
  val sample1 = Map("samples" -> Map("sample1" -> Map(
    "gender" -> "blablablablabla",
    "groups" -> List("1"),
    "libraries" -> Map(
      "lib1" -> Map("test" -> "1-1")
    )))
  )

  val sample2 = Map("samples" -> Map("sample2" -> Map(
    "groups" -> List("2"),
    "libraries" -> Map(
      "lib1" -> Map("test" -> "2-1", "groups" -> List("3")),
      "lib2" -> Map("test" -> "2-2")
    ))))

  val sample3 = Map("samples" -> Map("sample3" -> Map("libraries" -> Map(
    "lib1" -> Map("test" -> "3-1"),
    "lib2" -> Map("test" -> "3-2"),
    "lib3" -> Map("test" -> "3-3")
  ))))

  val child = Map("samples" -> Map("child" -> Map("gender" -> "male", "father" -> "father", "mother" -> "mother")))
  val father = Map("samples" -> Map("father" -> Map("gender" -> "male")))
  val mother = Map("samples" -> Map("mother" -> Map("gender" -> "female")))

  def apply(configs: List[Map[String, Any]], only: List[String] = Nil) = {
    new QScript with MultiSampleQScript { qscript =>

      override val onlySamples = only

      var buffer = new ListBuffer[String]()

      override def globalConfig = new Config(configs
        .foldLeft(Map[String, Any]()) { case (a, b) => ConfigUtils.mergeMaps(a, b)} )

      val root = null
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
      def summaryFile: File = null

      /** Init for pipeline */
      def init(): Unit = {
      }

      /** Pipeline itself */
      def biopetScript(): Unit = {
        addSamplesJobs()
      }
    }
  }
}