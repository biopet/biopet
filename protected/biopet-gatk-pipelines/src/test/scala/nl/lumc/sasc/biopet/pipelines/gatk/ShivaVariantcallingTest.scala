package nl.lumc.sasc.biopet.pipelines.gatk

import java.io.File

import com.google.common.io.Files
import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.gatk.broad.{ UnifiedGenotyper, HaplotypeCaller }
import nl.lumc.sasc.biopet.tools.{ VcfStats, MpileupToVcf, VcfFilter }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ DataProvider, Test, AfterClass }

import scala.collection.mutable.ListBuffer

/**
 * Created by pjvan_thof on 3/2/15.
 */
class ShivaVariantcallingTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): ShivaVariantcalling = {
    new ShivaVariantcalling() {
      override def configName = "shivavariantcalling"
      override def globalConfig = new Config(ConfigUtils.mergeMaps(map, ShivaVariantcallingTest.config))
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "shivaVariantcallingOptions")
  def shivaVariantcallingOptions = {
    val bool = Array(true, false)

    (for (
      bams <- 0 to 2;
      raw <- bool;
      bcftools <- bool;
      haplotypeCallerGvcf <- bool;
      haplotypeCallerAllele <- bool;
      unifiedGenotyperAllele <- bool;
      unifiedGenotyper <- bool;
      haplotypeCaller <- bool
    ) yield Array[Any](bams, raw, bcftools, unifiedGenotyper, haplotypeCaller, haplotypeCallerGvcf, haplotypeCallerAllele, unifiedGenotyperAllele)
    ).toArray
  }

  @Test(dataProvider = "shivaVariantcallingOptions")
  def testShivaVariantcalling(bams: Int,
                              raw: Boolean,
                              bcftools: Boolean,
                              unifiedGenotyper: Boolean,
                              haplotypeCaller: Boolean,
                              haplotypeCallerGvcf: Boolean,
                              haplotypeCallerAllele: Boolean,
                              unifiedGenotyperAllele: Boolean) = {
    val callers: ListBuffer[String] = ListBuffer()
    if (raw) callers.append("raw")
    if (bcftools) callers.append("bcftools")
    if (unifiedGenotyper) callers.append("unifiedgenotyper")
    if (haplotypeCallerGvcf) callers.append("haplotypecaller_gvcf")
    if (haplotypeCallerAllele) callers.append("haplotypecaller_allele")
    if (unifiedGenotyperAllele) callers.append("unifiedgenotyper_allele")
    if (haplotypeCaller) callers.append("haplotypecaller")
    val map = Map("variantcallers" -> callers.toList)
    val pipeline = initPipeline(map)

    pipeline.inputBams = (for (n <- 1 to bams) yield new File("bam_" + n + ".bam")).toList

    val illegalArgumentException = pipeline.inputBams.isEmpty ||
      (!raw && !bcftools &&
        !haplotypeCaller && !unifiedGenotyper &&
        !haplotypeCallerGvcf && !haplotypeCallerAllele && !unifiedGenotyperAllele)

    if (illegalArgumentException) intercept[IllegalArgumentException] {
      pipeline.script()
    }

    if (!illegalArgumentException) {
      pipeline.script()

      pipeline.functions.count(_.isInstanceOf[CombineVariants]) shouldBe 1 + (if (raw) 1 else 0)
      //pipeline.functions.count(_.isInstanceOf[Bcftools]) shouldBe (if (bcftools) 1 else 0)
      //FIXME: Can not check for bcftools because of piping
      pipeline.functions.count(_.isInstanceOf[MpileupToVcf]) shouldBe (if (raw) bams else 0)
      pipeline.functions.count(_.isInstanceOf[VcfFilter]) shouldBe (if (raw) bams else 0)
      pipeline.functions.count(_.isInstanceOf[HaplotypeCaller]) shouldBe (if (haplotypeCaller) 1 else 0) +
        (if (haplotypeCallerAllele) 1 else 0) + (if (haplotypeCallerGvcf) bams else 0)
      pipeline.functions.count(_.isInstanceOf[UnifiedGenotyper]) shouldBe (if (unifiedGenotyper) 1 else 0) +
        (if (unifiedGenotyperAllele) 1 else 0)
      pipeline.functions.count(_.isInstanceOf[VcfStats]) shouldBe (1 + callers.size)
    }
  }

  @AfterClass def removeTempOutputDir() = {
    FileUtils.deleteDirectory(ShivaVariantcallingTest.outputDir)
  }
}

object ShivaVariantcallingTest {
  val outputDir = Files.createTempDir()

  val config = Map(
    "name_prefix" -> "test",
    "output_dir" -> outputDir,
    "reference" -> "test",
    "gatk_jar" -> "test",
    "samtools" -> Map("exe" -> "test"),
    "bcftools" -> Map("exe" -> "test"),
    "input_alleles" -> "test"
  )
}