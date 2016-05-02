package nl.lumc.sasc.biopet.pipelines.toucan

import java.io.File
import java.nio.file.{Files, Paths}

import nl.lumc.sasc.biopet.extensions.VariantEffectPredictor
import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsView
import nl.lumc.sasc.biopet.extensions.manwe.{ManweAnnotateVcf, ManweSamplesImport}
import nl.lumc.sasc.biopet.extensions.tools.{GvcfToBed, VepNormalizer}
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by ahbbollen on 21-4-16.
 */
class ToucanTest extends TestNGSuite with Matchers {

  val faultyToucan = new Toucan(null) {
    override def sampleInfo: Map[String, Map[String, Any]] = Map(
      "sample1" -> Map("varda_group" -> List("group1", "group2"))
    )

    override def globalConfig = new Config(Map(
      "use_varda" -> true
    ))

    qSettings = new QSettings
    qSettings.runName = "test"
  }

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  faultyToucan.inputVCF = new File(resourcePath("/chrQ.vcf"))
  faultyToucan.inputGvcf = Some(new File(resourcePath("/chrQ.vcf")))

  @Test
  def testIncompletePipeline() = {
    an[IllegalStateException] should be thrownBy faultyToucan.script()
  }

  val correctToucan = new Toucan(null) {
    override def sampleInfo: Map[String, Map[String, Any]] = Map(
      "sample1" -> Map("varda_group" -> List("group1", "group2"))
    )

    override def globalConfig = new Config(Map(
      "use_varda" -> true,
      "output_dir" -> Files.createTempDirectory("test"),
      "varda_root" -> "http://teststest.test",
      "varda_token" -> "AAAAAAAAAAAAAAAA",
      "vep_script" -> "/path/to/vep",
      "manwe" -> Map("exe" -> "/bin/cat"),
      "database" -> true
    ))

    qSettings = new QSettings
    qSettings.runName = "test"
  }

  correctToucan.inputVCF = new File(resourcePath("/chrQ.vcf"))
  correctToucan.inputGvcf = Some(new File(resourcePath("/chrQ.vcf")))

  @Test
  def testCompletePipeline() = {
    noException should be thrownBy correctToucan.script()
  }

  @Test
  def testExpectedFunctions() = {
    correctToucan.functions.count(_.isInstanceOf[VariantEffectPredictor]) shouldBe 1
    correctToucan.functions.count(_.isInstanceOf[VepNormalizer]) shouldBe 1
    correctToucan.functions.count(_.isInstanceOf[ManweSamplesImport]) shouldBe 1
    correctToucan.functions.count(_.isInstanceOf[ManweActivateAfterAnnotImport]) shouldBe 1
    correctToucan.functions.count(_.isInstanceOf[ManweDownloadAfterAnnotate]) shouldBe 1
    correctToucan.functions.count(_.isInstanceOf[ManweAnnotateVcf]) shouldBe 1
    correctToucan.functions.count(_.isInstanceOf[GvcfToBed]) shouldBe 1
    correctToucan.functions.count(_.isInstanceOf[BcftoolsView]) shouldBe 1
  }

  @Test
  def testSamples() = {
    correctToucan.functions.
      filter(_.isInstanceOf[ManweSamplesImport]).
      map(x => x.asInstanceOf[ManweSamplesImport]).
      foreach(x => x.group shouldBe List("group1", "group2"))
  }


}
