package nl.lumc.sasc.biopet.pipelines.bammetrics

import java.io.{ FileOutputStream, File }

import com.google.common.io.Files
import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.extensions.bedtools.{ BedtoolsCoverage, BedtoolsIntersect }
import nl.lumc.sasc.biopet.extensions.picard._
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsFlagstat
import nl.lumc.sasc.biopet.scripts.CoverageStats
import nl.lumc.sasc.biopet.tools.BiopetFlagstat
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ Test, DataProvider, AfterClass }

/**
 * Created by pjvan_thof on 4/30/15.
 */
class BamMetricsTest extends TestNGSuite with Matchers {

  def initPipeline(map: Map[String, Any]): BamMetrics = {
    new BamMetrics() {
      override def configName = "bammetrics"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "bammetricsOptions")
  def bammetricsOptions = {
    val rois = Array(0, 1, 2, 3)
    val amplicon = Array(true, false)
    val rna = Array(true, false)

    for (
      rois <- rois;
      amplicon <- amplicon;
      rna <- rna
    ) yield Array(rois, amplicon, rna)
  }

  @Test(dataProvider = "bammetricsOptions")
  def testFlexiprep(rois: Int, amplicon: Boolean, rna: Boolean) = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> BamMetricsTest.outputDir),
      Map(BamMetricsTest.executables.toSeq: _*)) ++
      (if (amplicon) Map("amplicon_bed" -> "amplicon.bed") else Map()) ++
      (if (rna) Map("transcript_refflat" -> "transcripts.refFlat") else Map()) ++
      Map("regions_of_interest" -> (1 to rois).map("roi_" + _ + ".bed").toList)
    val bammetrics: BamMetrics = initPipeline(map)

    bammetrics.inputBam = new File("input.bam")
    bammetrics.sampleId = Some("1")
    bammetrics.libId = Some("1")
    bammetrics.script()

    var regions: Int = rois + (if (amplicon) 1 else 0)

    bammetrics.functions.count(_.isInstanceOf[CollectRnaSeqMetrics]) shouldBe (if (rna) 1 else 0)
    bammetrics.functions.count(_.isInstanceOf[CollectWgsMetrics]) shouldBe (if (rna) 0 else 1)
    bammetrics.functions.count(_.isInstanceOf[CollectMultipleMetrics]) shouldBe 1
    bammetrics.functions.count(_.isInstanceOf[CalculateHsMetrics]) shouldBe (if (amplicon) 1 else 0)
    bammetrics.functions.count(_.isInstanceOf[CollectTargetedPcrMetrics]) shouldBe (if (amplicon) 1 else 0)
    bammetrics.functions.count(_.isInstanceOf[BiopetFlagstat]) shouldBe (1 + (regions * 2))
    bammetrics.functions.count(_.isInstanceOf[SamtoolsFlagstat]) shouldBe (1 + (regions * 2))
    bammetrics.functions.count(_.isInstanceOf[BedtoolsIntersect]) shouldBe (regions * 2)

    bammetrics.functions.count(_.isInstanceOf[BedtoolsCoverage]) shouldBe regions
    bammetrics.functions.count(_.isInstanceOf[CoverageStats]) shouldBe regions
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass
  def removeTempOutputDir() = {
    FileUtils.deleteDirectory(BamMetricsTest.outputDir)
  }
}

object BamMetricsTest {
  val outputDir = Files.createTempDir()

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
