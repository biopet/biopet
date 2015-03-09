package nl.lumc.sasc.biopet.pipelines.gatk

import com.google.common.io.Files
import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.extensions.bwa.BwaMem
import nl.lumc.sasc.biopet.extensions.gatk.broad._
import nl.lumc.sasc.biopet.extensions.picard.{ MarkDuplicates, SortSam }
import nl.lumc.sasc.biopet.tools.VcfStats
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ Test, DataProvider }

/**
 * Created by pjvan_thof on 3/2/15.
 */
class ShivaTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): Shiva = {
    new Shiva() {
      override def configName = "shiva"
      override def globalConfig = new Config(ConfigUtils.mergeMaps(map, ShivaTest.config))
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "shivaOptions")
  def shivaOptions = {
    val bool = Array(true, false)

    for (
      s1 <- bool; s2 <- bool; s3 <- bool; multi <- bool; single <- bool;
      library <- bool; dbsnp <- bool; covariates <- bool; realign <- bool; baseRecalibration <- bool
    ) yield Array("", s1, s2, s3, multi, single, library, dbsnp, covariates, realign, baseRecalibration)
  }

  @Test(dataProvider = "shivaOptions")
  def testShiva(f: String, sample1: Boolean, sample2: Boolean, sample3: Boolean,
                multi: Boolean, single: Boolean, library: Boolean, dbsnp: Boolean,
                covariates: Boolean, realign: Boolean, baseRecalibration: Boolean): Unit = {
    val map = {
      var m: Map[String, Any] = ShivaTest.config
      if (sample1) m = ConfigUtils.mergeMaps(ShivaTest.sample1, m.toMap)
      if (sample2) m = ConfigUtils.mergeMaps(ShivaTest.sample2, m.toMap)
      if (sample3) m = ConfigUtils.mergeMaps(ShivaTest.sample3, m.toMap)
      if (dbsnp) m = ConfigUtils.mergeMaps(Map("dbsnp" -> "test"), m.toMap)
      ConfigUtils.mergeMaps(Map("multisample_sample_variantcalling" -> multi,
        "single_sample_variantcalling" -> single,
        "library_variantcalling" -> library,
        "use_analyze_covariates" -> covariates,
        "use_indel_realigner" -> realign,
        "use_base_recalibration" -> baseRecalibration), m.toMap)

    }

    if (!sample1 && !sample2 && !sample3) { // When no samples
      intercept[IllegalArgumentException] {
        initPipeline(map).script()
      }
    } else {
      val pipeline = initPipeline(map)
      pipeline.script()

      val numberLibs = (if (sample1) 1 else 0) + (if (sample2) 1 else 0) + (if (sample3) 2 else 0)
      val numberSamples = (if (sample1) 1 else 0) + (if (sample2) 1 else 0) + (if (sample3) 1 else 0)

      pipeline.functions.count(_.isInstanceOf[BwaMem]) shouldBe numberLibs
      pipeline.functions.count(_.isInstanceOf[SortSam]) shouldBe numberLibs
      pipeline.functions.count(_.isInstanceOf[MarkDuplicates]) shouldBe (numberLibs + (if (sample3) 1 else 0))

      // Gatk preprocess
      pipeline.functions.count(_.isInstanceOf[IndelRealigner]) shouldBe (numberLibs + (if (sample3) 1 else 0)) * (if (realign) 1 else 0)
      pipeline.functions.count(_.isInstanceOf[RealignerTargetCreator]) shouldBe (numberLibs + (if (sample3) 1 else 0)) * (if (realign) 1 else 0)
      pipeline.functions.count(_.isInstanceOf[BaseRecalibrator]) shouldBe (if (dbsnp && baseRecalibration) numberLibs else 0) * (if (covariates) 2 else 1)
      pipeline.functions.count(_.isInstanceOf[AnalyzeCovariates]) shouldBe (if (dbsnp && covariates && baseRecalibration) numberLibs else 0)
      pipeline.functions.count(_.isInstanceOf[PrintReads]) shouldBe (if (dbsnp && baseRecalibration) numberLibs else 0)

      pipeline.functions.count(_.isInstanceOf[VcfStats]) shouldBe (if (multi) 2 else 0) +
        (if (single) numberSamples * 2 else 0) + (if (library) numberLibs * 2 else 0)
    }
  }
}

object ShivaTest {
  val outputDir = Files.createTempDir()

  val config = Map(
    "name_prefix" -> "test",
    "output_dir" -> outputDir,
    "reference" -> "test",
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
}