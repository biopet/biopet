package nl.lumc.sasc.biopet.pipelines.tarmac

import nl.lumc.sasc.biopet.core.BiopetFifoPipe
import nl.lumc.sasc.biopet.extensions.{ Bgzip, Ln }
import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsSort
import nl.lumc.sasc.biopet.extensions.gatk.DepthOfCoverage
import nl.lumc.sasc.biopet.extensions.wisecondor.{ WisecondorCount, WisecondorGcCorrect, WisecondorNewRef }
import nl.lumc.sasc.biopet.extensions.xhmm.XhmmMergeGatkDepths
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by Sander Bollen on 13-4-17.
 */
class TarmacTest extends TestNGSuite with Matchers {
  import TarmacTest._

  def initPipeline(map: Map[String, Any]): Tarmac = {
    new Tarmac {
      override def configNamespace = "tarmac"

      override def globalConfig = new Config(map)

      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @Test
  def testSingleSampleLeft(): Unit = {
    val script = initPipeline(sample2)
    script.init()

    script.samples.size shouldBe 1
    script.samples.head._2.outputXhmmCountFile.isLeft shouldBe true
    script.samples.head._2.outputWisecondorCountFile.isLeft shouldBe true

  }

  @Test
  def testCountGeneration(): Unit = {
    val script = initPipeline(ConfigUtils.mergeMaps(samplesWithBam, settings))
    script.init()
    script.addSamplesJobs()

    script.samples.size shouldBe 7
    script.functions.count(_.isInstanceOf[WisecondorCount]) shouldBe 7
    script.functions.count(_.isInstanceOf[DepthOfCoverage]) shouldBe 7
    script.functions.count(_.isInstanceOf[WisecondorGcCorrect]) shouldBe 7

    val script2 = initPipeline(ConfigUtils.mergeMaps(samplesWithCount, settings))
    script2.init()
    script2.addSamplesJobs()

    script2.functions.count(_.isInstanceOf[WisecondorCount]) shouldBe 0
    script2.functions.count(_.isInstanceOf[DepthOfCoverage]) shouldBe 0
    script2.functions.count(_.isInstanceOf[Ln]) shouldBe 14
    script2.functions.count(_.isInstanceOf[WisecondorGcCorrect]) shouldBe 7
  }

  @Test
  def testReferenceSamples(): Unit = {
    val script = initPipeline(ConfigUtils.mergeMaps(samplesWithBam, settings))
    script.init()
    script.biopetScript()

    script.
      getReferenceSamplesForSample("sample1").
      getOrElse(Nil).toList.sorted shouldEqual List("sample4", "sample5").sorted
    script.
      getReferenceSamplesForSample("sample2").
      getOrElse(Nil).toList.sorted shouldEqual List("sample6", "sample7").sorted
    script.
      getReferenceSamplesForSample("sample3").
      getOrElse(Nil).toList.sorted shouldEqual List("sample1", "sample4", "sample5").sorted
    script.
      getReferenceSamplesForSample("sample4").
      getOrElse(Nil).toList.sorted shouldEqual List("sample1", "sample3", "sample5").sorted
    script.
      getReferenceSamplesForSample("sample5").
      getOrElse(Nil).toList.sorted shouldEqual List("sample1", "sample3", "sample4").sorted
    script.
      getReferenceSamplesForSample("sample6").
      getOrElse(Nil).toList.sorted shouldEqual List("sample2", "sample7").sorted
    script.
      getReferenceSamplesForSample("sample7").
      getOrElse(Nil).toList.sorted shouldEqual List("sample2", "sample6").sorted

  }

  @Test
  def testReferenceJobs(): Unit = {
    val script = initPipeline(ConfigUtils.mergeMaps(samplesWithBam, settings))
    script.init()
    script.biopetScript()

    script.functions.count(_.isInstanceOf[XhmmMergeGatkDepths]) shouldBe 7
    script.functions.count(_.isInstanceOf[BiopetFifoPipe]) shouldBe 7
    script.functions.collect {
      case b: BiopetFifoPipe =>
        b.beforeGraph()
        b.pipesJobs.count(_.isInstanceOf[WisecondorNewRef]) shouldBe 1
        b.pipesJobs.count(_.isInstanceOf[BedtoolsSort]) shouldBe 1
        b.pipesJobs.count(_.isInstanceOf[Bgzip]) shouldBe 1
    }
  }

}

object TarmacTest {

  val sample2 = Map("samples" ->
    Map("sample2" ->
      Map("tags" ->
        Map("gender" -> "male",
          "family" -> "fam01"
        )
      )
    )
  )

  val samplesWithBam = Map(
    "samples" -> Map(
      "sample1" -> Map(
        "tags" -> Map(
          "gender" -> "female",
          "father" -> "sample2",
          "mother" -> "sample3",
          "family" -> "fam01"
        ),
        "bam" -> "sample1.bam"
      ),
      "sample2" -> Map(
        "tags" -> Map(
          "gender" -> "male",
          "family" -> "fam01"
        ),
        "bam" -> "sample2.bam"
      ),
      "sample3" -> Map(
        "tags" -> Map(
          "gender" -> "female",
          "family" -> "fam01"
        ),
        "bam" -> "sample3.bam"
      ),
      "sample4" -> Map(
        "tags" -> Map(
          "gender" -> "female",
          "family" -> "fam02"
        ),
        "bam" -> "sample4.bam"
      ),
      "sample5" -> Map(
        "tags" -> Map(
          "gender" -> "female",
          "family" -> "fam02"
        ),
        "bam" -> "sample5.bam"
      ),
      "sample6" -> Map(
        "tags" -> Map(
          "gender" -> "male",
          "family" -> "fam02"
        ),
        "bam" -> "sample6.bam"
      ),
      "sample7" -> Map(
        "tags" -> Map(
          "gender" -> "male",
          "family" -> "fam02"
        ),
        "bam" -> "sample7.bam"
      )
    )
  )

  val samplesWithCount = Map(
    "samples" -> Map(
      "sample1" -> Map(
        "tags" -> Map(
          "gender" -> "female",
          "father" -> "sample2",
          "mother" -> "sample3",
          "family" -> "fam01"
        ),
        "xhmm_count_file" -> "sample1.xhmm",
        "wisecondor_count_file" -> "sample1.bed"
      ),
      "sample2" -> Map(
        "tags" -> Map(
          "gender" -> "male",
          "family" -> "fam01"
        ),
        "xhmm_count_file" -> "sample2.xhmm",
        "wisecondor_count_file" -> "sample2.bed"
      ),
      "sample3" -> Map(
        "tags" -> Map(
          "gender" -> "female",
          "family" -> "fam01"
        ),
        "xhmm_count_file" -> "sample3.xhmm",
        "wisecondor_count_file" -> "sample3.bed"
      ),
      "sample4" -> Map(
        "tags" -> Map(
          "gender" -> "female",
          "family" -> "fam02"
        ),
        "xhmm_count_file" -> "sample4.xhmm",
        "wisecondor_count_file" -> "sample4.bed"
      ),
      "sample5" -> Map(
        "tags" -> Map(
          "gender" -> "female",
          "family" -> "fam02"
        ),
        "xhmm_count_file" -> "sample5.xhmm",
        "wisecondor_count_file" -> "sample5.bed"
      ),
      "sample6" -> Map(
        "tags" -> Map(
          "gender" -> "male",
          "family" -> "fam02"
        ),
        "xhmm_count_file" -> "sample6.xhmm",
        "wisecondor_count_file" -> "sample6.bed"
      ),
      "sample7" -> Map(
        "tags" -> Map(
          "gender" -> "male",
          "family" -> "fam02"
        ),
        "xhmm_count_file" -> "sample7.xhmm",
        "wisecondor_count_file" -> "sample7.bed"
      )
    )
  )

  val settings = Map(
    "gatk_jar" -> "gatk.jar",
    "discover_params" -> "discover_params",
    "tarmac" -> Map(
      "targets" -> "targets.bed"
    )
  )
}
