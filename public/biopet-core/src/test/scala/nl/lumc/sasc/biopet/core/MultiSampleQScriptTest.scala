package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QScript
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.collection.mutable.{ListBuffer, StringBuilder}
import scalaz._
import Scalaz._

/**
  * Created by pjvan_thof on 12/29/15.
  */
class MultiSampleQScriptTest extends TestNGSuite with Matchers {
  import MultiSampleQScriptTest._

  @Test
  def test: Unit = {

    val bla = MultiSampleQScriptTest(sample1 :: sample2 :: sample3 :: child :: father :: mother :: Nil)
    bla.init()
    bla.biopetScript()

    bla.samples.foreach(_._2.gender)

    //println(bla.buffer.toString())
  }
}

object MultiSampleQScriptTest {
  val sample1 = Map("samples" -> Map("sample1" -> Map("libraries" -> Map(
    "lib1" -> Map("test" -> "1-1")
  ))))

  val sample2 = Map("samples" -> Map("sample2" -> Map("libraries" -> Map(
    "lib1" -> Map("test" -> "2-1"),
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

  def apply(configs: List[Map[String, Any]]) = {
    new QScript with MultiSampleQScript {

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