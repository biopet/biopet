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
package nl.lumc.sasc.biopet.pipelines.bammetrics

import java.io.{ File, FileOutputStream }

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions.picard._
import nl.lumc.sasc.biopet.extensions.tools.BamStats
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ AfterClass, DataProvider, Test }

/**
 * Test class for [[BamMetrics]]
 *
 * Created by pjvan_thof on 4/30/15.
 */
class BamMetricsTest extends TestNGSuite with Matchers {

  def initPipeline(map: Map[String, Any]): BamMetrics = {
    new BamMetrics() {
      override def configNamespace = "bammetrics"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "bammetricsOptions")
  def bammetricsOptions = {
    val rois = Array(0, 1, 2, 3)
    val bool = Array(true, false)

    for (
      rois <- rois;
      amplicon <- bool;
      rna <- bool;
      wgs <- bool
    ) yield Array(rois, amplicon, rna, wgs)
  }

  @Test(dataProvider = "bammetricsOptions")
  def testBamMetrics(rois: Int, amplicon: Boolean, rna: Boolean, wgs: Boolean) = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> BamMetricsTest.outputDir, "rna_metrics" -> rna, "wgs_metrics" -> wgs),
      Map(BamMetricsTest.executables.toSeq: _*)) ++
      (if (amplicon) Map("amplicon_bed" -> BamMetricsTest.ampliconBed.getAbsolutePath) else Map()) ++
      (if (rna) Map("annotation_refflat" -> "transcripts.refFlat") else Map()) ++
      Map("regions_of_interest" -> (1 to rois).map(BamMetricsTest.roi(_).getAbsolutePath).toList)
    val bammetrics: BamMetrics = initPipeline(map)

    bammetrics.inputBam = BamMetricsTest.bam
    bammetrics.sampleId = Some("1")
    bammetrics.libId = Some("1")
    bammetrics.script()

    var regions: Int = rois + (if (amplicon) 1 else 0)

    bammetrics.functions.count(_.isInstanceOf[CollectRnaSeqMetrics]) shouldBe (if (rna) 1 else 0)
    bammetrics.functions.count(_.isInstanceOf[CollectWgsMetrics]) shouldBe (if (wgs) 1 else 0)
    bammetrics.functions.count(_.isInstanceOf[CollectMultipleMetrics]) shouldBe 1
    bammetrics.functions.count(_.isInstanceOf[CalculateHsMetrics]) shouldBe (if (amplicon) 1 else 0)
    bammetrics.functions.count(_.isInstanceOf[CollectTargetedPcrMetrics]) shouldBe (if (amplicon) 1 else 0)
    bammetrics.functions.count(_.isInstanceOf[BamStats]) shouldBe 1
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass
  def removeTempOutputDir() = {
    FileUtils.deleteDirectory(BamMetricsTest.outputDir)
  }
}

object BamMetricsTest {
  val outputDir = Files.createTempDir()
  new File(outputDir, "input").mkdirs()

  val bam = new File(outputDir, "input" + File.separator + "bla.bam")
  Files.touch(bam)
  val ampliconBed = new File(outputDir, "input" + File.separator + "amplicon_bed.bed")
  Files.touch(ampliconBed)

  def roi(i: Int): File = {
    val roi = new File(outputDir, "input" + File.separator + s"roi${i}.bed")
    Files.touch(roi)
    roi
  }

  private def copyFile(name: String): Unit = {
    val is = getClass.getResourceAsStream("/" + name)
    val os = new FileOutputStream(new File(outputDir, name))
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  copyFile("ref.fa")
  copyFile("ref.dict")
  copyFile("ref.fa.fai")

  val executables = Map(
    "refFlat" -> "bla.refFlat",
    "reference_fasta" -> (outputDir + File.separator + "ref.fa"),
    "samtools" -> Map("exe" -> "test"),
    "bedtools" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test")
  )
}
