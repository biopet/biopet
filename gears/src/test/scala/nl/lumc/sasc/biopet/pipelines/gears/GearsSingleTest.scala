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

class GearsSingleTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): GearsSingle = {
    new GearsSingle {
      override def configNamespace = "gearssingle"

      override def globalConfig = new Config(map)

      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "gearsOptions")
  def gearsOptions = {
    val bool = Array(true, false)

    for (
      fromBam <- bool;
      pair <- bool;
      hasOutputName <- bool;
      kraken <- bool;
      qiimeClosed <- bool;
      qiimeRtax <- bool;
      seqCount <- bool
    ) yield Array("", fromBam, pair, hasOutputName, kraken, qiimeClosed, qiimeRtax, seqCount)
  }

  @Test(dataProvider = "gearsOptions")
  def testGears(dummy: String,
                fromBam: Boolean,
                paired: Boolean,
                hasOutputName: Boolean,
                kraken: Boolean,
                qiimeClosed: Boolean,
                qiimeRtax: Boolean,
                seqCount: Boolean) = {
    val map = ConfigUtils.mergeMaps(Map(
      "gears_use_kraken" -> kraken,
      "gears_use_qiime_rtax" -> qiimeRtax,
      "gears_use_qiime_closed" -> qiimeClosed,
      "gears_use_seq_count" -> seqCount,
      "output_dir" -> GearsSingleTest.outputDir
    ), Map(GearsSingleTest.executables.toSeq: _*))

    val gears: GearsSingle = initPipeline(map)
    gears.sampleId = Some("sampleName")
    gears.libId = Some("libName")

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

    gears.krakenScript.isDefined shouldBe kraken
    gears.qiimeClosed.isDefined shouldBe qiimeClosed
    gears.qiimeRatx.isDefined shouldBe qiimeRtax
    gears.seqCount.isDefined shouldBe seqCount

    // SamToFastq should have started if it was started from bam
    gears.functions.count(_.isInstanceOf[SamtoolsView]) shouldBe (if (fromBam) 1 else 0)
    gears.functions.count(_.isInstanceOf[SamToFastq]) shouldBe (if (fromBam) 1 else 0)

    gears.functions.count(_.isInstanceOf[Kraken]) shouldBe (if (kraken) 1 else 0)
    gears.functions.count(_.isInstanceOf[KrakenReport]) shouldBe (if (kraken) 1 else 0)
    gears.functions.count(_.isInstanceOf[KrakenReportToJson]) shouldBe (if (kraken) 1 else 0)
  }

  @Test
  def testNoSample: Unit = {
    val map = ConfigUtils.mergeMaps(Map(
      "output_dir" -> GearsSingleTest.outputDir
    ), Map(GearsSingleTest.executables.toSeq: _*))
    val gears: GearsSingle = initPipeline(map)

    intercept[IllegalArgumentException] {
      gears.script()
    }
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
  r1.deleteOnExit()
  val r2 = new File(outputDir, "input" + File.separator + "R2.fq")
  Files.touch(r2)
  r2.deleteOnExit()
  val bam = new File(outputDir, "input" + File.separator + "bamfile.bam")
  Files.touch(bam)
  bam.deleteOnExit()

  val executables = Map(
    "kraken" -> Map("exe" -> "test", "db" -> "test"),
    "krakenreport" -> Map("exe" -> "test", "db" -> "test"),
    "sambamba" -> Map("exe" -> "test"),
    "samtools" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test"),
    "assigntaxonomy" -> Map("exe" -> "test"),
    "pickclosedreferenceotus" -> Map("exe" -> "test"),
    "pickotus" -> Map("exe" -> "test"),
    "pickrepset" -> Map("exe" -> "test"),
    "splitlibrariesfastq" -> Map("exe" -> "test"),
    "flash" -> Map("exe" -> "test"),
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "sickle" -> Map("exe" -> "test"),
    "cutadapt" -> Map("exe" -> "test")
  )
}
