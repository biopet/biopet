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
package nl.lumc.sasc.biopet.pipelines.sage

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.utils.{ConfigUtils, Logging}
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{AfterClass, DataProvider, Test}

/**
 * Created by pjvanthof on 28/09/16.
 */
class SageTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): Sage = {
    new Sage() {
      override def configNamespace = "sage"

      override def globalConfig = new Config(ConfigUtils.mergeMaps(map, SageTest.config))

      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "sageOptions")
  def sageOptions = {
    for (
      s1 <- sample1; s2 <- sample2
    ) yield Array("", s1, s2)
  }

  def sample1 = Array(false, true)
  def sample2 = Array(false, true)
  def transcriptome = true
  def countBed = true
  def tagsLibrary = false
  def libraryCounts: Option[Boolean] = None

  private var dirs: List[File] = Nil

  @Test(dataProvider = "sageOptions")
  def testSage(f: String, sample1: Boolean, sample2: Boolean): Unit = {
    val outputDir = SageTest.outputDir
    dirs :+= outputDir
    val map = {
      var m: Map[String, Any] = SageTest.config(outputDir)
      if (sample1) m = ConfigUtils.mergeMaps(SageTest.sample1, m)
      if (sample2) m = ConfigUtils.mergeMaps(SageTest.sample2, m)
      ConfigUtils.mergeMaps(
        (if (transcriptome) Map[String, Any]("transcriptome" -> SageTest.inputTouch("trans.fa")) else Map[String, Any]()) ++
          (if (countBed) Map[String, Any]("count_bed" -> SageTest.inputTouch("count.bed")) else Map[String, Any]()) ++
          (if (tagsLibrary) Map[String, Any]("tags_library" -> SageTest.inputTouch("tablib")) else Map[String, Any]()) ++
          libraryCounts.map("library_counts" -> _),
        m)

    }

    if ((!sample1 && !sample2) || !countBed || (!transcriptome && !tagsLibrary)) {
      // When no samples
      intercept[IllegalStateException] {
        initPipeline(map).script()
      }
      Logging.errors.clear()
    } else {
      val pipeline = initPipeline(map)
      pipeline.script()

      val numberLibs = (if (sample1) 1 else 0) + (if (sample2) 2 else 0)
      val numberSamples = (if (sample1) 1 else 0) + (if (sample2) 1 else 0)

      pipeline.summaryFiles shouldBe Map()
      pipeline.summarySettings shouldBe Map()

      pipeline.samples.foreach {
        case (sampleId, sample) =>
          sample.summaryFiles shouldBe Map()
          sample.summaryStats shouldBe Map()
          sample.summarySettings shouldBe Map()
          sample.libraries.foreach {
            case (libId, lib) =>
              lib.summaryFiles shouldBe Map()
              lib.summaryStats shouldBe Map()
              lib.summarySettings shouldBe Map()
          }
      }

    }
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    dirs.foreach(FileUtils.deleteDirectory)
  }
}

class SageNoBedTest extends SageTest {
  override def sample1 = Array(true)
  override def sample2 = Array(false)
  override def countBed = false
}

class SageNoLibTest extends SageTest {
  override def sample1 = Array(true)
  override def sample2 = Array(false)
  override def transcriptome = false
  override def tagsLibrary = false
}

class SageLibraryCountsTest extends SageTest {
  override def sample1 = Array(true)
  override def sample2 = Array(false)
  override def libraryCounts = Some(true)
}

object SageTest {
  def outputDir = Files.createTempDir()
  val intputDir = Files.createTempDir()

  def inputTouch(name: String): String = {
    val file = new File(intputDir, name)
    Files.touch(file)
    file.getAbsolutePath
  }

  inputTouch("ref.ebwt")

  private def copyFile(name: String): Unit = {
    val is = getClass.getResourceAsStream("/" + name)
    val os = new FileOutputStream(new File(intputDir, name))
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  copyFile("ref.fa")
  copyFile("ref.dict")
  copyFile("ref.fa.fai")

  def config(outputDir: File): Map[String, Any] = Map(
    "skip_write_dependencies" -> true,
    "reference_fasta" -> (intputDir + File.separator + "ref.fa"),
    "output_dir" -> outputDir.getAbsolutePath,
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test"),
    "bedtools" -> Map("exe" -> "test"),
    "bowtie" -> Map("exe" -> "test"),
    "bowtie_index" -> inputTouch("ref")
  )

  val sample1 = Map(
    "samples" -> Map("sample1" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "R1" -> inputTouch("1_1_R1.fq"),
        "R2" -> inputTouch("1_1_R2.fq")
      )
    )
    )))

  val sample2 = Map(
    "samples" -> Map("sample3" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "R1" -> inputTouch("2_1_R1.fq"),
        "R2" -> inputTouch("2_1_R2.fq")
      ),
      "lib2" -> Map(
        "R1" -> inputTouch("2_2_R1.fq"),
        "R2" -> inputTouch("2_2_R2.fq")
      )
    )
    )))

}