package nl.lumc.sasc.biopet.pipelines.carp

import java.io.File

import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.testng.annotations.{ AfterClass, Test, DataProvider }
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite

import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.extensions.bwa.BwaMem
import nl.lumc.sasc.biopet.extensions.macs2.Macs2CallPeak
import nl.lumc.sasc.biopet.extensions.picard.{ MergeSamFiles, SortSam }
import nl.lumc.sasc.biopet.utils.ConfigUtils

/**
 * Created by pjvan_thof on 2/13/15.
 */
class CarpTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): Carp = {
    new Carp() {
      override def configName = "carp"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "carpOptions", parallel = true)
  def carpOptions = {
    val bool = Array(true, false)

    for (s1 <- bool; s2 <- bool; s3 <- bool; t <- bool; c <- bool) yield Array("", s1, s2, s3, t, c)
  }

  @Test(dataProvider = "carpOptions")
  def testCarp(f: String, sample1: Boolean, sample2: Boolean, sample3: Boolean, threatment: Boolean, control: Boolean): Unit = {
    val map = {
      var m = ConfigUtils.mergeMaps(Map("output_dir" -> CarpTest.outputDir
      ), CarpTest.excutables)

      if (sample1) m = ConfigUtils.mergeMaps(CarpTest.sample1, m.toMap)
      if (sample2) m = ConfigUtils.mergeMaps(CarpTest.sample2, m.toMap)
      if (sample3) m = ConfigUtils.mergeMaps(CarpTest.sample3, m.toMap)
      if (threatment) m = ConfigUtils.mergeMaps(CarpTest.threatment1, m.toMap)
      if (control) m = ConfigUtils.mergeMaps(CarpTest.control1, m.toMap)
      m
    }

    if (!sample1 && !sample2 && !sample3 && !threatment && !control) { // When no samples
      intercept[IllegalArgumentException] {
        initPipeline(map).script()
      }
    } else if (threatment && !control) { // If control of a samples does not exist in samples
      intercept[IllegalStateException] {
        initPipeline(map).script()
      }
    } else { // When samples are correct
      val carp = initPipeline(map)
      carp.script()
      val numberLibs = (if (sample1) 1 else 0) + (if (sample2) 1 else 0) + (if (sample3) 2 else 0) +
        (if (threatment) 1 else 0) + (if (control) 1 else 0)
      val numberSamples = (if (sample1) 1 else 0) + (if (sample2) 1 else 0) + (if (sample3) 1 else 0) +
        (if (threatment) 1 else 0) + (if (control) 1 else 0)

      carp.functions.count(_.isInstanceOf[BwaMem]) shouldBe numberLibs
      carp.functions.count(_.isInstanceOf[SortSam]) shouldBe numberLibs
      carp.functions.count(_.isInstanceOf[MergeSamFiles]) shouldBe (if (sample3) 1 else 0)

      carp.functions.count(_.isInstanceOf[Macs2CallPeak]) shouldBe (numberSamples + (if (threatment) 1 else 0))
    }
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    FileUtils.deleteDirectory(CarpTest.outputDir)
  }
}

object CarpTest {
  val outputDir = Files.createTempDir()

  val excutables = Map(
    "reference" -> "test",
    "seqstat" -> Map("exe" -> "test"),
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "sickle" -> Map("exe" -> "test"),
    "bwa" -> Map("exe" -> "test"),
    "samtools" -> Map("exe" -> "test"),
    "macs2" -> Map("exe" -> "test"),
    "igvtools" -> Map("exe" -> "test"),
    "wigtobigwig" -> Map("exe" -> "test")
  )

  val sample1 = Map(
    "samples" -> Map("sample1" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "R1" -> "1_1_R1.fq",
        "R2" -> "1_1_R2.fq"
      )
    )
    )))

  val sample2 = Map(
    "samples" -> Map("sample2" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "R1" -> "2_1_R1.fq",
        "R2" -> "2_1_R2.fq"
      )
    )
    )))

  val sample3 = Map(
    "samples" -> Map("sample3" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "R1" -> "3_1_R1.fq",
        "R2" -> "3_1_R2.fq"
      ),
      "lib2" -> Map(
        "R1" -> "3_2_R1.fq",
        "R2" -> "3_2_R2.fq"
      )
    )
    )))

  val threatment1 = Map(
    "samples" -> Map("threatment" -> Map("control" -> "control1", "libraries" -> Map(
      "lib1" -> Map(
        "R1" -> "threatment_1_R1.fq",
        "R2" -> "threatment_1_R2.fq"
      )
    )
    )))

  val control1 = Map(
    "samples" -> Map("control1" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "R1" -> "control_1_R1.fq",
        "R2" -> "control_1_R2.fq"
      )
    )
    )))

}