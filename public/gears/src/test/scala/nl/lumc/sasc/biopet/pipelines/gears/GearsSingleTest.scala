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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.gears

import java.io.File

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions.kraken.{ Kraken, KrakenReport }
import nl.lumc.sasc.biopet.extensions.picard.SamToFastq
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsView
import nl.lumc.sasc.biopet.extensions.tools.KrakenReportToJson
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations._

/**
 * Test class for [[GearsSingle]]
 *
 * Created by wyleung on 10/22/15.
 */

class GearsSingleTest(val testset: String) extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): GearsSingle = {
    new GearsSingle {
      override def configName = "gears"

      override def globalConfig = new Config(map)

      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "gearsOptions")
  def gearsOptions = {
    val startFromBam = Array(true, false)
    val paired = Array(true, false)
    val hasOutputNames = Array(true, false)
    val hasFileExtensions = Array(true, false)

    for (
      fromBam <- startFromBam;
      pair <- paired;
      hasOutputName <- hasOutputNames
    ) yield Array(testset, fromBam, pair, hasOutputName)
  }

  @Test(dataProvider = "gearsOptions")
  def testGears(testset: String, fromBam: Boolean, paired: Boolean,
                hasOutputName: Boolean) = {
    val map = ConfigUtils.mergeMaps(Map(
      "output_dir" -> GearsSingleTest.outputDir
    ), Map(GearsSingleTest.executables.toSeq: _*))

    val gears: GearsSingle = initPipeline(map)

    if (fromBam) {
      gears.bamFile = Some(GearsSingleTest.bam)
    } else {
      gears.fastqR1 = Some(GearsSingleTest.r1)
      gears.fastqR2 = if (paired) Some(GearsSingleTest.r2) else None
    }
    if (hasOutputName)
      gears.outputName = "test"

    gears.script()

    if (hasOutputName) {
      gears.outputName shouldBe "test"
    } else {
      // in the following cases the filename should have been determined by the filename
      gears.outputName shouldBe (if (fromBam) "bamfile" else "R1")
    }

    // SamToFastq should have started if it was started from bam
    gears.functions.count(_.isInstanceOf[SamtoolsView]) shouldBe (if (fromBam) 1 else 0)
    gears.functions.count(_.isInstanceOf[SamToFastq]) shouldBe (if (fromBam) 1 else 0)

    gears.functions.count(_.isInstanceOf[Kraken]) shouldBe 1
    gears.functions.count(_.isInstanceOf[KrakenReport]) shouldBe 1
    gears.functions.count(_.isInstanceOf[KrakenReportToJson]) shouldBe 1
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    FileUtils.deleteDirectory(GearsSingleTest.outputDir)
  }
}

object GearsSingleTest {
  val outputDir = Files.createTempDir()
  new File(outputDir, "input").mkdirs()

  val r1 = new File(outputDir, "input" + File.separator + "R1.fq")
  Files.touch(r1)
  val r2 = new File(outputDir, "input" + File.separator + "R2.fq")
  Files.touch(r2)
  val bam = new File(outputDir, "input" + File.separator + "bamfile.bam")
  Files.touch(bam)

  val executables = Map(
    "kraken" -> Map("exe" -> "test", "db" -> "test"),
    "krakenreport" -> Map("exe" -> "test", "db" -> "test"),
    "sambamba" -> Map("exe" -> "test"),
    "samtools" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test")
  )
}
