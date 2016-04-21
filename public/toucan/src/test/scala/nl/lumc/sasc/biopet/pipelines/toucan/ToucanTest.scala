package nl.lumc.sasc.biopet.pipelines.toucan

import java.io.File
import java.nio.file.{Files, Paths}

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
      "sample1" -> Map("varda_groups" -> List("group1", "group2"))
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
    an [IllegalStateException] should be thrownBy faultyToucan.script()
  }


  val correctToucan = new Toucan(null) {
    override def sampleInfo: Map[String, Map[String, Any]] = Map(
      "sample1" -> Map("varda_groups" -> List("group1", "group2"))
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
}
