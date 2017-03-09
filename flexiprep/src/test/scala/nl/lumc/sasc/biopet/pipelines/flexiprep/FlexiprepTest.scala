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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.File

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions.tools.{ SeqStat, ValidateFastq }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ AfterClass, DataProvider, Test }

/**
 * Test class for [[Flexiprep]]
 *
 * Created by pjvan_thof on 2/11/15.
 */
class FlexiprepTest extends TestNGSuite with Matchers {

  def initPipeline(map: Map[String, Any]): Flexiprep = {
    new Flexiprep() {
      override def configNamespace = "flexiprep"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "flexiprepOptions")
  def flexiprepOptions = {
    val paired = Array(true, false)
    val skipTrims = Array(true, false)
    val skipClips = Array(true, false)
    val zipped = Array(true, false)
    val abortOnCorruptFastqs = Array(true, false)

    for (
      pair <- paired;
      skipTrim <- skipTrims;
      skipClip <- skipClips;
      zip <- zipped;
      abortOnCorruptFastq <- abortOnCorruptFastqs
    ) yield Array("", pair, skipTrim, skipClip, zip, abortOnCorruptFastq)
  }

  private var dirs: List[File] = Nil

  @Test(dataProvider = "flexiprepOptions")
  def testFlexiprep(f: String, paired: Boolean,
                    skipTrim: Boolean,
                    skipClip: Boolean,
                    zipped: Boolean,
                    abortOnCorruptFastq: Boolean) = {
    val outputDir = FlexiprepTest.outputDir
    dirs :+= outputDir
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> outputDir,
      "skip_trim" -> skipTrim,
      "skip_clip" -> skipClip,
      "abort_on_corrupt_fastq" -> abortOnCorruptFastq
    ), Map(FlexiprepTest.executables.toSeq: _*))
    val flexiprep: Flexiprep = initPipeline(map)

    flexiprep.inputR1 = (if (zipped) FlexiprepTest.r1Zipped else FlexiprepTest.r1)
    if (paired) flexiprep.inputR2 = Some((if (zipped) FlexiprepTest.r2Zipped else FlexiprepTest.r2))
    flexiprep.sampleId = Some("1")
    flexiprep.libId = Some("1")
    flexiprep.script()

    flexiprep.functions.count(_.isInstanceOf[Fastqc]) shouldBe (if (paired) 4 else 2)
    flexiprep.functions.count(_.isInstanceOf[SeqStat]) shouldBe (if (paired) 4 else 2)

    flexiprep.functions.count(_.isInstanceOf[ValidateFastq]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[CheckValidateFastq]) shouldBe (if (abortOnCorruptFastq) 2 else 0)

  }

  @Test
  def testNoSample: Unit = {
    val outputDir = FlexiprepTest.outputDir
    dirs :+= outputDir
    val map = ConfigUtils.mergeMaps(Map(
      "output_dir" -> outputDir
    ), Map(FlexiprepTest.executables.toSeq: _*))
    val flexiprep: Flexiprep = initPipeline(map)

    intercept[IllegalStateException] {
      flexiprep.script()
    }
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    dirs.foreach(FileUtils.deleteDirectory)
  }
}

object FlexiprepTest {
  def outputDir = Files.createTempDir()

  val inputDir = Files.createTempDir()

  val r1 = new File(inputDir, "R1.fq")
  Files.touch(r1)
  r1.deleteOnExit()
  val r2 = new File(inputDir, "R2.fq")
  Files.touch(r2)
  r2.deleteOnExit()
  val r1Zipped = new File(inputDir, "R1.fq.gz")
  Files.touch(r1Zipped)
  r1Zipped.deleteOnExit()
  val r2Zipped = new File(inputDir, "R2.fq.gz")
  Files.touch(r2Zipped)
  r2Zipped.deleteOnExit()

  val executables = Map(
    "skip_write_dependencies" -> true,
    "seqstat" -> Map("exe" -> "test"),
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "sickle" -> Map("exe" -> "test"),
    "cutadapt" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test")
  )
}
