package nl.lumc.sasc.biopet.pipelines.basty

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions.{Raxml, RunGubbins}
import nl.lumc.sasc.biopet.extensions.gatk.{BaseRecalibrator, IndelRealigner, PrintReads, RealignerTargetCreator}
import nl.lumc.sasc.biopet.extensions.picard.MarkDuplicates
import nl.lumc.sasc.biopet.extensions.tools.{BastyGenerateFasta, VcfStats}
import nl.lumc.sasc.biopet.utils.{ConfigUtils, Logging}
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{DataProvider, Test}

/**
  * Created by pjvanthof on 27/09/16.
  */
class BastyTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): Basty = {
    new Basty() {
      override def configNamespace = "shiva"
      override def globalConfig = new Config(ConfigUtils.mergeMaps(map, BastyTest.config))
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }


  @DataProvider(name = "bastyOptions")
  def bastyOptions = {
    for (
      s1 <- sample1; s2 <- sample2
    ) yield Array("", s1, s2)
  }

  def sample1 = Array(false, true)
  def sample2 = Array(false, true)
  def realign = true
  def baseRecalibration = true
  def multisampleCalling: Boolean = true
  def sampleCalling = false
  def libraryCalling = false
  def dbsnp = false
  def svCalling = false
  def cnvCalling = false
  def annotation = false
  def bootRuns: Option[Int] = None

  @Test(dataProvider = "bastyOptions")
  def testBasty(f: String, sample1: Boolean, sample2: Boolean): Unit = {
    val map = {
      var m: Map[String, Any] = BastyTest.config
      if (sample1) m = ConfigUtils.mergeMaps(BastyTest.sample1, m)
      if (sample2) m = ConfigUtils.mergeMaps(BastyTest.sample2, m)
      if (dbsnp) m = ConfigUtils.mergeMaps(Map("dbsnp" -> "test.vcf.gz"), m)
      ConfigUtils.mergeMaps(Map(
        "multisample_variantcalling" -> multisampleCalling,
        "single_sample_variantcalling" -> sampleCalling,
        "library_variantcalling" -> libraryCalling,
        "use_indel_realigner" -> realign,
        "use_base_recalibration" -> baseRecalibration,
        "sv_calling" -> svCalling,
        "cnv_calling" -> cnvCalling,
        "annotation" -> annotation,
        "boot_runs" -> bootRuns), m)

    }

    if (!sample1 && !sample2) { // When no samples
      intercept[IllegalArgumentException] {
        initPipeline(map).script()
      }
      Logging.errors.clear()
    } else {
      val pipeline = initPipeline(map)
      pipeline.script()

      val numberLibs = (if (sample1) 1 else 0) + (if (sample2) 2 else 0)
      val numberSamples = (if (sample1) 1 else 0) + (if (sample2) 1 else 0)

      pipeline.functions.count(_.isInstanceOf[MarkDuplicates]) shouldBe (numberLibs + (if (sample2) 1 else 0))

      // Gatk preprocess
      pipeline.functions.count(_.isInstanceOf[IndelRealigner]) shouldBe (numberLibs * (if (realign) 1 else 0) + (if (sample2 && realign) 1 else 0))
      pipeline.functions.count(_.isInstanceOf[RealignerTargetCreator]) shouldBe (numberLibs * (if (realign) 1 else 0) + (if (sample2 && realign) 1 else 0))
      pipeline.functions.count(_.isInstanceOf[BaseRecalibrator]) shouldBe (if (dbsnp && baseRecalibration) (numberLibs * 2) else 0)
      pipeline.functions.count(_.isInstanceOf[PrintReads]) shouldBe (if (dbsnp && baseRecalibration) numberLibs else 0)

      pipeline.summarySettings.get("annotation") shouldBe Some(annotation)
      pipeline.summarySettings.get("sv_calling") shouldBe Some(svCalling)
      pipeline.summarySettings.get("cnv_calling") shouldBe Some(cnvCalling)

      pipeline.samples foreach {
        case (sampleId, sample) =>
          sample.summarySettings.get("single_sample_variantcalling") shouldBe Some(sampleCalling)
          sample.summarySettings.get("use_indel_realigner") shouldBe Some(realign)
          sample.libraries.foreach {
            case (libId, lib) =>
              lib.summarySettings.get("library_variantcalling") shouldBe Some(libraryCalling)
              lib.summarySettings.get("use_indel_realigner") shouldBe Some(realign)
              lib.summarySettings.get("use_base_recalibration") shouldBe Some(baseRecalibration && dbsnp)
          }
      }

      pipeline.functions.count(_.isInstanceOf[VcfStats]) shouldBe (
        (if (multisampleCalling) 2 else 0) +
          (if (sampleCalling) numberSamples * 2 else 0) +
          (if (libraryCalling) numberLibs * 2 else 0))

      pipeline.functions.count(_.isInstanceOf[BastyGenerateFasta]) shouldBe 2 + (2 * numberSamples)
      pipeline.functions.count(_.isInstanceOf[Raxml]) shouldBe (2 * (2 + bootRuns.getOrElse(100)))
      pipeline.functions.count(_.isInstanceOf[RunGubbins]) shouldBe 2
    }
  }
}

object BastyTest {
  val outputDir = Files.createTempDir()
  outputDir.deleteOnExit()
  new File(outputDir, "input").mkdirs()
  def inputTouch(name: String): String = {
    val file = new File(outputDir, "input" + File.separator + name)
    Files.touch(file)
    file.getAbsolutePath
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

  val config = Map(
    "name_prefix" -> "test",
    "cache" -> true,
    "dir" -> "test",
    "vep_script" -> "test",
    "output_dir" -> outputDir,
    "reference_fasta" -> (outputDir + File.separator + "ref.fa"),
    "gatk_jar" -> "test",
    "samtools" -> Map("exe" -> "test"),
    "bcftools" -> Map("exe" -> "test"),
    "fastqc" -> Map("exe" -> "test"),
    "input_alleles" -> "test",
    "variantcallers" -> "raw",
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "sickle" -> Map("exe" -> "test"),
    "cutadapt" -> Map("exe" -> "test"),
    "bwa" -> Map("exe" -> "test"),
    "samtools" -> Map("exe" -> "test"),
    "macs2" -> Map("exe" -> "test"),
    "igvtools" -> Map("exe" -> "test", "igvtools_jar" -> "test"),
    "wigtobigwig" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test"),
    "bgzip" -> Map("exe" -> "test"),
    "tabix" -> Map("exe" -> "test"),
    "breakdancerconfig" -> Map("exe" -> "test"),
    "breakdancercaller" -> Map("exe" -> "test"),
    "pindelconfig" -> Map("exe" -> "test"),
    "pindelcaller" -> Map("exe" -> "test"),
    "pindelvcf" -> Map("exe" -> "test"),
    "clever" -> Map("exe" -> "test"),
    "delly" -> Map("exe" -> "test"),
    "rungubbins" -> Map("exe" -> "test"),
    "raxml" -> Map("exe" -> "test"),
    "pysvtools" -> Map(
      "exe" -> "test",
      "exclusion_regions" -> "test",
      "translocations_only" -> false),
    "freec" -> Map(
      "exe" -> "test",
      "chrFiles" -> "test",
      "chrLenFile" -> "test"
    )
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