/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk

import java.io.{ File, FileOutputStream }

import com.google.common.io.Files
import nl.lumc.sasc.biopet.utils.config.Config
import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.gatk.broad.{ HaplotypeCaller, UnifiedGenotyper }
import nl.lumc.sasc.biopet.extensions.tools.{ MpileupToVcf, VcfFilter, VcfStats }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ AfterClass, DataProvider, Test }

import scala.collection.mutable.ListBuffer

/**
 * Class for testing ShivaVariantcalling
 *
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

    pipeline.inputBams = (for (n <- 1 to bams) yield ShivaVariantcallingTest.inputTouch("bam_" + n + ".bam")).toList

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
      //pipeline.functions.count(_.isInstanceOf[MpileupToVcf]) shouldBe (if (raw) bams else 0)
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
  new File(outputDir, "input").mkdirs()
  def inputTouch(name: String): File = {
    val file = new File(outputDir, "input" + File.separator + name).getAbsoluteFile
    Files.touch(file)
    file
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
    "output_dir" -> outputDir,
    "cache" -> true,
    "dir" -> "test",
    "vep_script" -> "test",
    "reference" -> (outputDir + File.separator + "ref.fa"),
    "reference_fasta" -> (outputDir + File.separator + "ref.fa"),
    "gatk_jar" -> "test",
    "samtools" -> Map("exe" -> "test"),
    "bcftools" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test"),
    "bgzip" -> Map("exe" -> "test"),
    "tabix" -> Map("exe" -> "test"),
    "input_alleles" -> "test"
  )
}