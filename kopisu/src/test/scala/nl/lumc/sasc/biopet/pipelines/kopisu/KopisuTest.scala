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
package nl.lumc.sasc.biopet.pipelines.kopisu

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions.freec.{FreeC, FreeCAssessSignificancePlot, FreeCCNVPlot}
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{AfterClass, DataProvider, Test}

import scala.collection.mutable.ListBuffer

/**
 * Test class for [[Kopisu]]
 *
 * Created by pjvan_thof on 3/2/15.
 */
class KopisuTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any], outputDir: File): Kopisu = {
    new Kopisu() {
      override def configNamespace = "kopisu"
      override def globalConfig = new Config(ConfigUtils.mergeMaps(map, KopisuTest.config(outputDir)))
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  private var dirs: List[File] = Nil

  @DataProvider(name = "shivaSvCallingOptions")
  def shivaSvCallingOptions = {
    val bool = Array(true, false)
    (for (
      bams <- 0 to 3;
      freec <- bool;
      conifer <- bool
    ) yield Array(bams, freec, conifer)).toArray
  }

  @Test(dataProvider = "shivaSvCallingOptions")
  def testShivaSvCalling(bams: Int,
                         freec: Boolean,
                         conifer: Boolean) = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir

    val callers: ListBuffer[String] = ListBuffer()
    val map = Map("sv_callers" -> callers.toList)
    val pipeline = initPipeline(map ++ Map(
      "use_freec_method" -> freec,
      "use_conifer_method" -> conifer
    ), outputDir)

    pipeline.inputBams = (for (n <- 1 to bams) yield n.toString -> KopisuTest.inputTouch("bam_" + n + ".bam")).toMap

    val illegalArgumentException = pipeline.inputBams.isEmpty || (!freec && !conifer)

    if (illegalArgumentException) intercept[IllegalStateException] {
      pipeline.init()
      pipeline.script()
    }

    if (!illegalArgumentException) {
      pipeline.init()
      pipeline.script()

      pipeline.freecMethod.isDefined shouldBe freec
      pipeline.summarySettings.get("freec_method") shouldBe Some(freec)

      pipeline.functions.count(_.isInstanceOf[FreeC]) shouldBe (if (freec) bams else 0)
      pipeline.functions.count(_.isInstanceOf[FreeCAssessSignificancePlot]) shouldBe (if (freec) bams else 0)
      pipeline.functions.count(_.isInstanceOf[FreeCCNVPlot]) shouldBe (if (freec) bams else 0)
    }
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    dirs.foreach(FileUtils.deleteDirectory)
  }
}

object KopisuTest {
  def outputDir = Files.createTempDir()
  val inputDir = Files.createTempDir()

  private def inputTouch(name: String): File = {
    val file = new File(inputDir, name).getAbsoluteFile
    Files.touch(file)
    file
  }

  private def copyFile(name: String): Unit = {
    val is = getClass.getResourceAsStream("/" + name)
    val os = new FileOutputStream(new File(inputDir, name))
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  copyFile("ref.fa")
  copyFile("ref.dict")
  copyFile("ref.fa.fai")

  val controlDir = Files.createTempDir()
  Files.touch(new File(controlDir, "test.txt"))

  def config(outputDir: File) = Map(
    "skip_write_dependencies" -> true,
    "name_prefix" -> "test",
    "output_dir" -> outputDir,
    "reference_fasta" -> (inputDir + File.separator + "ref.fa"),
    "gatk_jar" -> "test",
    "samtools" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test"),
    "bgzip" -> Map("exe" -> "test"),
    "tabix" -> Map("exe" -> "test"),
    "freec" -> Map("exe" -> "test", "chrFiles" -> "test", "chrLenFile" -> "test"),
    "controls_dir" -> controlDir.getAbsolutePath,
    "conifer" -> Map("script" -> "/usr/bin/test"),
    "probe_file" -> "test",
    "rscript" -> Map("exe" -> "test")
  )
}