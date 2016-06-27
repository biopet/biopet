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
import nl.lumc.sasc.biopet.extensions.kraken.{Kraken, KrakenReport}
import nl.lumc.sasc.biopet.extensions.picard.SamToFastq
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsView
import nl.lumc.sasc.biopet.extensions.tools.KrakenReportToJson
import nl.lumc.sasc.biopet.utils.{ConfigUtils, Logging}
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations._

/**
 * Test class for [[GearsSingle]]
 *
 * Created by wyleung on 10/22/15.
 */

abstract class TestGearsSingle extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): GearsSingle = {
    new GearsSingle {
      override def configNamespace = "gearssingle"

      override def globalConfig = new Config(map)

      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  def paired: Boolean = false
  def hasOutputName: Boolean = false
  def kraken: Option[Boolean] = None
  def qiimeClosed: Boolean = false
  def qiimeOpen: Boolean = false
  def qiimeRtax: Boolean = false
  def seqCount: Boolean = false
  def downsample: Option[Int] = None

  def inputMode: Option[String] = Some("fastq")

  @Test
  def testGears(): Unit = {
    val map = ConfigUtils.mergeMaps(Map(
      "gears_use_qiime_rtax" -> qiimeRtax,
      "gears_use_qiime_closed" -> qiimeClosed,
      "gears_use_qiime_open" -> qiimeOpen,
      "gears_use_seq_count" -> seqCount,
      "output_dir" -> TestGearsSingle.outputDir
    ) ++
      kraken.map("gears_use_kraken" -> _) ++
      downsample.map("downsample" -> _),
      Map(TestGearsSingle.executables.toSeq: _*))

    val gears: GearsSingle = initPipeline(map)
    gears.sampleId = Some("sampleName")
    gears.libId = Some("libName")

    inputMode match {
      case Some("fastq") =>
        gears.fastqR1 = Some(TestGearsSingle.r1)
        gears.fastqR2 = if (paired) Some(TestGearsSingle.r2) else None
      case Some("bam") => gears.bamFile = Some(TestGearsSingle.bam)
      case None        =>
      case _           => new IllegalStateException(s"$inputMode not allowed as inputMode")
    }

    if (hasOutputName)
      gears.outputName = "test"

    if (inputMode.isEmpty) {
      intercept[IllegalArgumentException] {
        gears.script()
      }
      Logging.errors.clear()
    } else {

      gears.script()

      if (hasOutputName) {
        gears.outputName shouldBe "test"
      } else {
        // in the following cases the filename should have been determined by the filename
        gears.outputName shouldBe (if (inputMode == Some("bam")) "bamfile" else "R1")
      }

      gears.summarySettings("gears_use_kraken") shouldBe kraken.getOrElse(true)
      gears.summarySettings("gear_use_qiime_rtax") shouldBe qiimeRtax
      gears.summarySettings("gear_use_qiime_closed") shouldBe qiimeClosed
      gears.summarySettings("gear_use_qiime_open") shouldBe qiimeOpen

      gears.krakenScript.isDefined shouldBe kraken.getOrElse(true)
      gears.qiimeClosed.isDefined shouldBe qiimeClosed
      gears.qiimeOpen.isDefined shouldBe qiimeOpen
      gears.qiimeRatx.isDefined shouldBe qiimeRtax
      gears.seqCount.isDefined shouldBe seqCount

      // SamToFastq should have started if it was started from bam
      gears.functions.count(_.isInstanceOf[SamtoolsView]) shouldBe (if (inputMode == Some("bam")) 1 else 0)
      gears.functions.count(_.isInstanceOf[SamToFastq]) shouldBe (if (inputMode == Some("bam")) 1 else 0)

      gears.functions.count(_.isInstanceOf[Kraken]) shouldBe (if (kraken.getOrElse(true)) 1 else 0)
      gears.functions.count(_.isInstanceOf[KrakenReport]) shouldBe (if (kraken.getOrElse(true)) 1 else 0)
      gears.functions.count(_.isInstanceOf[KrakenReportToJson]) shouldBe (if (kraken.getOrElse(true)) 1 else 0)
    }
  }
}

class GearsSingleNoInputTest extends TestGearsSingle {
  override def inputMode = None
}

class GearsSingleDefaultTest extends TestGearsSingle
class GearsSingleKrakenTest extends TestGearsSingle {
  override def kraken = Some(true)
}
class GearsSingleQiimeClosedTest extends TestGearsSingle {
  override def qiimeClosed = true
}
class GearsSingleQiimeOpenTest extends TestGearsSingle {
  override def qiimeOpen = true
}
class GearsSingleQiimeRtaxTest extends TestGearsSingle {
  override def qiimeRtax = true
}
class GearsSingleseqCountTest extends TestGearsSingle {
  override def seqCount = true
}

class GearsSingleKrakenPairedTest extends TestGearsSingle {
  override def paired = true
  override def kraken = Some(true)
}
class GearsSingleQiimeClosedPairedTest extends TestGearsSingle {
  override def paired = true
  override def qiimeClosed = true
}
class GearsSingleQiimeOpenPairedTest extends TestGearsSingle {
  override def paired = true
  override def qiimeOpen = true
}
class GearsSingleQiimeRtaxPairedTest extends TestGearsSingle {
  override def paired = true
  override def qiimeRtax = true
}
class GearsSingleseqCountPairedTest extends TestGearsSingle {
  override def paired = true
  override def seqCount = true
}

class GearsSingleAllTest extends TestGearsSingle {
  override def kraken = Some(true)
  override def qiimeClosed = true
  override def qiimeOpen = true
  override def qiimeRtax = true
  override def seqCount = true
}
class GearsSingleAllPairedTest extends TestGearsSingle {
  override def kraken = Some(true)
  override def qiimeClosed = true
  override def qiimeOpen = true
  override def qiimeRtax = true
  override def seqCount = true
  override def paired = true
}

class GearsSingleBamTest extends TestGearsSingle {
  override def inputMode = Some("bam")
}

class GearsSingleQiimeClosedDownsampleTest extends TestGearsSingle {
  override def paired = true
  override def qiimeClosed = true
  override def downsample = Some(10000)
}
class GearsSingleQiimeOpenDownsampleTest extends TestGearsSingle {
  override def paired = true
  override def qiimeOpen = true
  override def downsample = Some(10000)
}

object TestGearsSingle {
  val outputDir = Files.createTempDir()
  outputDir.deleteOnExit()
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
    "pickopenreferenceotus" -> Map("exe" -> "test"),
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
