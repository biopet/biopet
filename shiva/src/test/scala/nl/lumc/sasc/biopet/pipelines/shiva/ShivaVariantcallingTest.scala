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
/**
  * Due to the license issue with GATK, this part of Biopet can only be used inside the
  * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
  * on how to use this protected part of biopet or contact us at sasc@lumc.nl
  */
package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.core.BiopetPipe
import nl.lumc.sasc.biopet.extensions.Freebayes
import nl.lumc.sasc.biopet.extensions.bcftools.{BcftoolsCall, BcftoolsMerge}
import nl.lumc.sasc.biopet.extensions.gatk._
import nl.lumc.sasc.biopet.utils.config.Config
import nl.lumc.sasc.biopet.extensions.tools.{MpileupToVcf, VcfFilter, VcfStats}
import nl.lumc.sasc.biopet.extensions.vt.{VtDecompose, VtNormalize}
import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.somatic.TumorNormalPair
import nl.lumc.sasc.biopet.utils.{ConfigUtils, Logging}
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{AfterClass, DataProvider, Test}

import scala.collection.mutable.ListBuffer

/**
  * Class for testing ShivaVariantcalling
  *
  * Created by pjvan_thof on 3/2/15.
  */
trait ShivaVariantcallingTestTrait extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any], dir: File): ShivaVariantcalling = {
    new ShivaVariantcalling() {
      override def configNamespace = "shivavariantcalling"
      override def globalConfig =
        new Config(ConfigUtils.mergeMaps(map, ShivaVariantcallingTest.config(dir)))
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  def raw: Boolean = false
  def mutect2: Boolean = false
  def bcftools: Boolean = false
  def bcftools_singlesample: Boolean = false
  def haplotypeCallerGvcf: Boolean = false
  def haplotypeCallerAllele: Boolean = false
  def unifiedGenotyperAllele: Boolean = false
  def unifiedGenotyper: Boolean = false
  def haplotypeCaller: Boolean = false
  def freebayes: Boolean = false
  def varscanCnsSinglesample: Boolean = false
  def referenceVcf: Option[File] = None
  def roiBedFiles: List[File] = Nil
  def ampliconBedFile: Option[File] = None
  def runContest: Option[Boolean] = None

  def normalize = false
  def decompose = false

  def bamRange: List[Int] = (0 to 2).toList

  def tumorNormalPairs: List[TumorNormalPair] = Nil

  @DataProvider(name = "shivaVariantcallingOptions")
  def shivaVariantcallingOptions: Array[Array[Any]] = {
    (for (bams <- bamRange)
      yield
        Array[Any](
          bams,
          raw,
          mutect2,
          bcftools,
          bcftools_singlesample,
          unifiedGenotyper,
          haplotypeCaller,
          haplotypeCallerGvcf,
          haplotypeCallerAllele,
          unifiedGenotyperAllele,
          freebayes,
          varscanCnsSinglesample,
          tumorNormalPairs
        )).toArray
  }

  private var dirs: List[File] = Nil

  @Test(dataProvider = "shivaVariantcallingOptions")
  def testShivaVariantcalling(bams: Int,
                              raw: Boolean,
                              mutect2: Boolean,
                              bcftools: Boolean,
                              bcftoolsSinglesample: Boolean,
                              unifiedGenotyper: Boolean,
                              haplotypeCaller: Boolean,
                              haplotypeCallerGvcf: Boolean,
                              haplotypeCallerAllele: Boolean,
                              unifiedGenotyperAllele: Boolean,
                              freebayes: Boolean,
                              varscanCnsSinglesample: Boolean,
                              tumorNormalPairs: List[TumorNormalPair]): Unit = {
    val outputDir = ShivaVariantcallingTest.outputDir
    dirs :+= outputDir
    val callers: ListBuffer[String] = ListBuffer()
    if (raw) callers.append("raw")
    if (mutect2) callers.append("mutect2")
    if (bcftools) callers.append("bcftools")
    if (bcftoolsSinglesample) callers.append("bcftools_singlesample")
    if (unifiedGenotyper) callers.append("unifiedgenotyper")
    if (haplotypeCallerGvcf) callers.append("haplotypecaller_gvcf")
    if (haplotypeCallerAllele) callers.append("haplotypecaller_allele")
    if (unifiedGenotyperAllele) callers.append("unifiedgenotyper_allele")
    if (haplotypeCaller) callers.append("haplotypecaller")
    if (freebayes) callers.append("freebayes")
    if (varscanCnsSinglesample) callers.append("varscan_cns_singlesample")
    val sampleTags: Map[String, Any] = tumorNormalPairs.foldLeft(Map[String, Any]()) {
      case (m, pair) =>
        val tag = Map(
          "samples" -> Map(pair.tumorSample -> Map(
            "tags" -> Map("type" -> "tumor", "normal" -> pair.normalSample))))
        ConfigUtils.mergeMaps(m, tag)
    }
    val map = sampleTags ++ Map(
      "variantcallers" -> callers.toList,
      "execute_vt_normalize" -> normalize,
      "execute_vt_decompose" -> decompose,
      "regions_of_interest" -> roiBedFiles.map(_.getAbsolutePath)
    ) ++ referenceVcf.map("reference_vcf" -> _) ++ ampliconBedFile.map(
      "amplicon_bed" -> _.getAbsolutePath) ++ runContest.map("run_contest" -> _)
    val pipeline = initPipeline(map, outputDir)

    pipeline.inputBams = (for (n <- 1 to bams)
      yield
        s"sample_${n.toString}" -> ShivaVariantcallingTest.inputTouch("bam_" + n + ".bam")).toMap

    val illegalArgumentException = pipeline.inputBams.isEmpty || callers.isEmpty
    val illegalStateException = mutect2 && bams == 1

    if (illegalArgumentException) intercept[IllegalArgumentException] {
      pipeline.script()
    } else if (illegalStateException) intercept[IllegalStateException] {
      pipeline.script()
    } else {
      pipeline.script()

      val pipesJobs = pipeline.functions
        .filter(_.isInstanceOf[BiopetPipe])
        .flatMap(_.asInstanceOf[BiopetPipe].pipesJobs)

      pipeline.functions.count(_.isInstanceOf[CombineVariants]) shouldBe (1 + (if (raw) 1 else 0) + (if (varscanCnsSinglesample)
                                                                                                       1
                                                                                                     else
                                                                                                       0))
      pipesJobs.count(_.isInstanceOf[BcftoolsCall]) shouldBe (if (bcftools) 1 else 0) + (if (bcftoolsSinglesample)
                                                                                           bams
                                                                                         else 0)
      pipeline.functions.count(_.isInstanceOf[BcftoolsMerge]) shouldBe (if (bcftoolsSinglesample && bams > 1)
                                                                          1
                                                                        else 0)
      pipesJobs.count(_.isInstanceOf[Freebayes]) shouldBe (if (freebayes) 1 else 0)
      pipesJobs.count(_.isInstanceOf[MpileupToVcf]) shouldBe (if (raw) bams else 0)
      pipeline.functions.count(_.isInstanceOf[VcfFilter]) shouldBe (if (raw) bams else 0)
      pipeline.functions.count(_.isInstanceOf[HaplotypeCaller]) shouldBe (if (haplotypeCaller) 1
                                                                          else 0) +
        (if (haplotypeCallerAllele) 1 else 0) + (if (haplotypeCallerGvcf) bams else 0)
      pipeline.functions.count(_.isInstanceOf[UnifiedGenotyper]) shouldBe (if (unifiedGenotyper) 1
                                                                           else 0) +
        (if (unifiedGenotyperAllele) 1 else 0)
      pipeline.functions.count(_.isInstanceOf[VcfStats]) shouldBe (1 + callers.size + (roiBedFiles ++ ampliconBedFile).length * (1 + callers.size))
      pipeline.functions.count(_.isInstanceOf[VtNormalize]) shouldBe (if (normalize) callers.size
                                                                      else 0)
      pipeline.functions.count(_.isInstanceOf[VtDecompose]) shouldBe (if (decompose) callers.size
                                                                      else 0)
      pipeline.functions.count(_.isInstanceOf[GenotypeConcordance]) shouldBe (if (referenceVcf.isDefined)
                                                                                1 + callers.size
                                                                              else 0)
      pipesJobs.count(_.isInstanceOf[MuTect2]) shouldBe (if (mutect2) tumorNormalPairs.size else 0)
      pipeline.functions.count(_.isInstanceOf[ContEst]) shouldBe (if (mutect2 && runContest
                                                                        .getOrElse(false))
                                                                    tumorNormalPairs.size
                                                                  else 0)

      pipeline.summarySettings
        .get("variantcallers")
        .map(_.asInstanceOf[List[String]].toSet) shouldBe Some(callers.toSet)
      pipeline.summarySettings.get("amplicon_bed") shouldBe Some(
        ampliconBedFile.map(_.getAbsolutePath))
      pipeline.summarySettings.get("regions_of_interest") shouldBe Some(
        roiBedFiles.map(_.getAbsolutePath))
    }
    Logging.errors.clear()
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir(): Unit = {
    dirs.foreach(FileUtils.deleteDirectory)
  }
}

class ShivaVariantcallingNoVariantcallersTest extends ShivaVariantcallingTestTrait
class ShivaVariantcallingAllTest extends ShivaVariantcallingTestTrait {
  override def raw: Boolean = true
  override def bcftools: Boolean = true
  override def bcftools_singlesample: Boolean = true
  override def haplotypeCallerGvcf: Boolean = true
  override def haplotypeCallerAllele: Boolean = true
  override def unifiedGenotyperAllele: Boolean = true
  override def unifiedGenotyper: Boolean = true
  override def haplotypeCaller: Boolean = true
  override def freebayes: Boolean = true
  override def varscanCnsSinglesample: Boolean = true
}
class ShivaVariantcallingRawTest extends ShivaVariantcallingTestTrait {
  override def raw: Boolean = true
}
class ShivaVariantcallingMuTect2Test extends ShivaVariantcallingTestTrait {
  override def mutect2: Boolean = true
  override def haplotypeCaller: Boolean = true
  override def tumorNormalPairs: List[TumorNormalPair] =
    TumorNormalPair("sample_1", "sample_2") :: Nil
}
class ShivaVariantcallingMuTect2ContestTest extends ShivaVariantcallingTestTrait {
  override def mutect2: Boolean = true
  override def haplotypeCaller: Boolean = true
  override def runContest = Some(true)
  override def tumorNormalPairs: List[TumorNormalPair] =
    TumorNormalPair("sample_1", "sample_2") :: Nil
}
class ShivaVariantcallingBcftoolsTest extends ShivaVariantcallingTestTrait {
  override def bcftools: Boolean = true
}
class ShivaVariantcallingBcftoolsSinglesampleTest extends ShivaVariantcallingTestTrait {
  override def bcftools_singlesample: Boolean = true
}
class ShivaVariantcallingHaplotypeCallerGvcfTest extends ShivaVariantcallingTestTrait {
  override def haplotypeCallerGvcf: Boolean = true
}
class ShivaVariantcallingHaplotypeCallerAlleleTest extends ShivaVariantcallingTestTrait {
  override def haplotypeCallerAllele: Boolean = true
}
class ShivaVariantcallingUnifiedGenotyperAlleleTest extends ShivaVariantcallingTestTrait {
  override def unifiedGenotyperAllele: Boolean = true
}
class ShivaVariantcallingUnifiedGenotyperTest extends ShivaVariantcallingTestTrait {
  override def unifiedGenotyper: Boolean = true
}
class ShivaVariantcallingHaplotypeCallerTest extends ShivaVariantcallingTestTrait {
  override def haplotypeCaller: Boolean = true
}
class ShivaVariantcallingFreebayesTest extends ShivaVariantcallingTestTrait {
  override def freebayes: Boolean = true
}
class ShivaVariantcallingVarscanCnsSinglesampleTest extends ShivaVariantcallingTestTrait {
  override def varscanCnsSinglesample: Boolean = true
}
class ShivaVariantcallingNormalizeTest extends ShivaVariantcallingTestTrait {
  override def unifiedGenotyper: Boolean = true
  override def normalize = true
}
class ShivaVariantcallingDecomposeTest extends ShivaVariantcallingTestTrait {
  override def unifiedGenotyper: Boolean = true
  override def decompose = true
}
class ShivaVariantcallingNormalizeDecomposeTest extends ShivaVariantcallingTestTrait {
  override def unifiedGenotyper: Boolean = true
  override def normalize = true
  override def decompose = true
}
class ShivaVariantcallingReferenceVcfTest extends ShivaVariantcallingTestTrait {
  override def unifiedGenotyper: Boolean = true
  override def referenceVcf = Some(new File("ref.vcf"))
}
class ShivaVariantcallingAmpliconTest extends ShivaVariantcallingTestTrait {
  override def unifiedGenotyper: Boolean = true
  override def ampliconBedFile = Some(new File("amplicon.bed"))
}

object ShivaVariantcallingTest {
  def outputDir: File = Files.createTempDir()
  val inputDir: File = Files.createTempDir()

  def inputTouch(name: String): File = {
    val file = new File(inputDir, name).getAbsoluteFile
    Files.touch(file)
    file
  }

  private def copyFile(name: String): Unit = {
    val is = getClass.getResourceAsStream("/" + name)
    val os = new FileOutputStream(new File(inputDir, name))
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  copyFile("ref.fa")
  copyFile("ref.dict")
  copyFile("ref.fa.fai")

  def config(outputDir: File) = Map(
    "skip_write_dependencies" -> true,
    "name_prefix" -> "test",
    "output_dir" -> outputDir,
    "cache" -> true,
    "dir" -> "test",
    "vep_script" -> "test",
    "reference_fasta" -> (inputDir + File.separator + "ref.fa"),
    "gatk_jar" -> "test",
    "samtools" -> Map("exe" -> "test"),
    "bcftools" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test"),
    "bgzip" -> Map("exe" -> "test"),
    "tabix" -> Map("exe" -> "test"),
    "input_alleles" -> "test.vcf.gz",
    "varscan_jar" -> "test",
    "vt" -> Map("exe" -> "test"),
    "popfile" -> "test"
  )
}
