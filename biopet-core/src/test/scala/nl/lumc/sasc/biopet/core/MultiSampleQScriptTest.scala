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
package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.core.MultiSampleQScript.Gender
import nl.lumc.sasc.biopet.core.extensions.Md5sum
import nl.lumc.sasc.biopet.utils.{ ConfigUtils, Logging }
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.{ QScript, QSettings }
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.language.reflectiveCalls
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

    script.functions.size shouldBe 5

    script.samples.foreach {
      case (sampleId, sample) =>
        sample.gender shouldBe Gender.Unknown
        sample.father shouldBe None
        sample.mother shouldBe None
        sample.summaryFiles shouldBe Map()
        sample.summaryStats shouldBe Map()
        sample.summarySettings shouldBe Map()
        sample.sampleDir shouldBe new File(script.outputDir, "samples" + File.separator + sampleId)
        sample.createFile("bla.txt") shouldBe new File(sample.sampleDir, s"$sampleId.bla.txt")

        sample.libraries.foreach {
          case (libId, library) =>
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

    script.functions.size shouldBe 5

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

    script.functions.size shouldBe 5

    script.samples("sample1").sampleGroups shouldBe List("1")
    script.samples("sample1").libraries("lib1").libGroups should not be List("1")
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

  @Test
  def testInvalidSampleName: Unit = {
    val script = MultiSampleQScriptTest(sample4 :: Nil)
    script.init()
    script.biopetScript()
    val msg = script.getLastLogMessage
    msg shouldBe "Sample 'Something.Invalid' has an invalid name. " +
      "Sample names must have at least 3 characters, " +
      "must begin and end with an alphanumeric character, " +
      "and must not have whitespace and special characters. " +
      "Dash (-) and underscore (_) are permitted."

  }

  @Test
  def testNoLibSample(): Unit = {
    an[IllegalStateException] shouldBe thrownBy(MultiSampleQScriptTest(Map("output_dir" -> ".") :: noLibSample :: Nil).script())
  }
}

object MultiSampleQScriptTest {
  val sample1 = Map("samples" -> Map("sample1" -> Map(
    "tags" -> Map(
      "gender" -> "blablablablabla",
      "groups" -> List("1")
    ),
    "libraries" -> Map(
      "lib1" -> Map("test" -> "1-1")
    )))
  )

  val sample2 = Map("samples" -> Map("sample2" -> Map(
    "tags" -> Map(
      "groups" -> List("2")
    ),
    "libraries" -> Map(
      "lib1" -> Map("test" -> "2-1", "tags" -> Map(
        "groups" -> List("3")
      )),
      "lib2" -> Map("test" -> "2-2")
    ))))

  val sample3 = Map("samples" -> Map("sample3" -> Map("libraries" -> Map(
    "lib1" -> Map("test" -> "3-1"),
    "lib2" -> Map("test" -> "3-2"),
    "lib3" -> Map("test" -> "3-3")
  ))))

  val sample4 = Map("samples" -> Map("Something.Invalid" -> Map("libraries" -> Map(
    "lib1" -> Map("test" -> "4-1")
  ))))

  val child = Map(
    "samples" -> Map(
      "child" -> Map(
        "tags" -> Map(
          "gender" -> "male",
          "father" -> "father",
          "mother" -> "mother"
        ),
        "libraries" -> Map(
          "lib1" -> Map("test" -> "child-1")
        )
      )
    )
  )
  val father = Map(
    "samples" -> Map(
      "father" -> Map(
        "tags" -> Map("gender" -> "male"),
        "libraries" -> Map(
          "lib1" -> Map("test" -> "father-1")
        )
      )
    )
  )
  val mother = Map(
    "samples" -> Map(
      "mother" -> Map(
        "tags" -> Map("gender" -> "female"),
        "libraries" -> Map(
          "lib1" -> Map("test" -> "mother-1")
        )
      )
    )
  )

  val noLibSample = Map(
    "samples" -> Map(
      "sample1" -> Map(
        "tags" -> Map("gender" -> "female")
      )
    )
  )

  def apply(configs: List[Map[String, Any]], only: List[String] = Nil) = {
    new QScript with MultiSampleQScript { qscript =>

      qSettings = new QSettings()
      qSettings.runName = "test"

      override val onlySamples = only

      var buffer = new ListBuffer[String]()

      override def globalConfig = new Config(configs
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
      def init(): Unit = {
      }

      /** Pipeline itself */
      def biopetScript(): Unit = {
        addSamplesJobs()
        addSummaryJobs()
      }
    }
  }
}