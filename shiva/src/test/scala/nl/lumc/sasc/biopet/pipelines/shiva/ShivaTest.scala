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
package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.{File, FileOutputStream, IOException}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions.gatk.{
  BaseRecalibrator,
  IndelRealigner,
  PrintReads,
  RealignerTargetCreator
}
import nl.lumc.sasc.biopet.extensions.picard.MarkDuplicates
import nl.lumc.sasc.biopet.extensions.tools.VcfStats
import nl.lumc.sasc.biopet.utils.{ConfigUtils, Logging}
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{AfterClass, DataProvider, Test}

/**
  * Class for testing shiva
  *
  * Created by pjvan_thof on 3/2/15.
  */
trait ShivaTestTrait extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): Shiva = {
    new Shiva() {
      override def configNamespace = "shiva"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "shivaOptions")
  def shivaOptions: Array[Array[Any]] = {
    for (s1 <- sample1; s2 <- sample2;
         realign <- realignProvider; baseRecalibration <- baseRecalibrationProvider)
      yield Array("", s1, s2, realign, baseRecalibration)
  }

  def sample1 = Array(false, true)
  def sample2 = Array(false, true)
  def realignProvider = Array(false, true)
  def baseRecalibrationProvider = Array(false, true)
  def multisampleCalling: Boolean = true
  def dbsnp = true
  def svCalling = false
  def cnvCalling = false
  def annotation = false
  def usePrintReads = true

  private var dirs: List[File] = Nil

  @Test(dataProvider = "shivaOptions")
  def testShiva(f: String,
                sample1: Boolean,
                sample2: Boolean,
                realign: Boolean,
                baseRecalibration: Boolean): Unit = {
    val outputDir = ShivaTest.outputDir
    dirs :+= outputDir
    val map = {
      var m: Map[String, Any] = ShivaTest.config(outputDir)
      if (sample1) m = ConfigUtils.mergeMaps(ShivaTest.sample1, m)
      if (sample2) m = ConfigUtils.mergeMaps(ShivaTest.sample2, m)
      if (dbsnp) m = ConfigUtils.mergeMaps(Map("dbsnp_vcf" -> "test.vcf.gz"), m)
      ConfigUtils.mergeMaps(
        Map(
          "multisample_variantcalling" -> multisampleCalling,
          "use_indel_realigner" -> realign,
          "use_base_recalibration" -> baseRecalibration,
          "sv_calling" -> svCalling,
          "cnv_calling" -> cnvCalling,
          "annotation" -> annotation,
          "use_printreads" -> usePrintReads
        ),
        m
      )

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

      pipeline.functions.count(_.isInstanceOf[MarkDuplicates]) shouldBe (numberLibs + numberSamples)

      // Gatk preprocess
      pipeline.functions.count(_.isInstanceOf[IndelRealigner]) shouldBe (if (realign) numberSamples
                                                                         else 0)
      pipeline.functions.count(_.isInstanceOf[RealignerTargetCreator]) shouldBe (if (realign)
                                                                                   numberSamples
                                                                                 else 0)
      pipeline.functions.count(_.isInstanceOf[BaseRecalibrator]) shouldBe (if (dbsnp && baseRecalibration)
                                                                             numberLibs * 2
                                                                           else 0)
      pipeline.functions.count(_.isInstanceOf[PrintReads]) shouldBe (if (dbsnp && baseRecalibration && usePrintReads)
                                                                       numberLibs
                                                                     else 0)

      pipeline.summarySettings.get("annotation") shouldBe Some(annotation)
      pipeline.summarySettings.get("sv_calling") shouldBe Some(svCalling)
      pipeline.summarySettings.get("cnv_calling") shouldBe Some(cnvCalling)

      pipeline.samples foreach {
        case (_, sample) =>
          sample.summarySettings.get("single_sample_variantcalling") shouldBe None
          sample.summarySettings.get("use_indel_realigner") shouldBe Some(realign)
          sample.libraries.foreach {
            case (_, lib) =>
              lib.summarySettings.get("library_variantcalling") shouldBe None
              lib.summarySettings.get("use_indel_realigner") shouldBe None // Should not exist anymore
              lib.summarySettings.get("use_base_recalibration") shouldBe Some(
                baseRecalibration && dbsnp)
          }
      }

      pipeline.functions.count(_.isInstanceOf[VcfStats]) shouldBe (if (multisampleCalling) 2 else 0)
    }
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir(): Unit = {
    dirs.filter(_.exists()).foreach { dir =>
      try {
        FileUtils.deleteDirectory(dir)
      } catch {
        case e: IOException if e.getMessage.startsWith("Unable to delete directory") =>
          Logging.logger.error(e.getMessage)
      }
    }
  }
}

class ShivaDefaultTest extends ShivaTestTrait
class ShivaNoDbsnpTest extends ShivaTestTrait {
  override def sample1 = Array(x = true)
  override def sample2 = Array(x = false)
  override def realignProvider = Array(x = true)
  override def dbsnp = false
}
class ShivaNoPrintReadsTest extends ShivaTestTrait {
  override def realignProvider = Array(x = false)
  override def usePrintReads = false
}
class ShivaWithSvCallingTest extends ShivaTestTrait {
  override def sample1 = Array(x = true)
  override def sample2 = Array(x = false)
  override def realignProvider = Array(x = false)
  override def baseRecalibrationProvider = Array(x = false)
  override def svCalling = true
}
class ShivaWithCnvCallingTest extends ShivaTestTrait {
  override def sample1 = Array(x = true)
  override def sample2 = Array(x = false)
  override def realignProvider = Array(x = false)
  override def baseRecalibrationProvider = Array(x = false)
  override def cnvCalling = true
}
class ShivaWithAnnotationTest extends ShivaTestTrait {
  override def sample1 = Array(x = true)
  override def sample2 = Array(x = false)
  override def realignProvider = Array(x = false)
  override def baseRecalibrationProvider = Array(x = false)
  override def annotation = true
}

object ShivaTest {
  def outputDir: File = Files.createTempDir()

  val inputDir: File = Files.createTempDir()

  def inputTouch(name: String): String = {
    val file = new File(inputDir, name)
    Files.touch(file)
    file.deleteOnExit()
    file.getAbsolutePath
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
    "cache" -> true,
    "dir" -> "test",
    "vep_script" -> "test",
    "output_dir" -> outputDir,
    "reference_fasta" -> (inputDir + File.separator + "ref.fa"),
    "gatk_jar" -> "test",
    "samtools" -> Map("exe" -> "test"),
    "bcftools" -> Map("exe" -> "test"),
    "fastqc" -> Map("exe" -> "test"),
    "input_alleles" -> "test",
    "variantcallers" -> "raw",
    "rscript" -> Map("exe" -> "test"),
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
    "pysvtools" -> Map("exe" -> "test",
                       "exclusion_regions" -> "test",
                       "translocations_only" -> false),
    "freec" -> Map(
      "exe" -> "test",
      "chrFiles" -> "test",
      "chrLenFile" -> "test"
    )
  )

  val sample1 = Map(
    "samples" -> Map(
      "sample1" -> Map(
        "libraries" -> Map(
          "lib1" -> Map(
            "R1" -> inputTouch("1_1_R1.fq"),
            "R2" -> inputTouch("1_1_R2.fq")
          )
        ))))

  val sample2 = Map(
    "samples" -> Map(
      "sample3" -> Map(
        "libraries" -> Map(
          "lib1" -> Map(
            "R1" -> inputTouch("2_1_R1.fq"),
            "R2" -> inputTouch("2_1_R2.fq")
          ),
          "lib2" -> Map(
            "R1" -> inputTouch("2_2_R1.fq"),
            "R2" -> inputTouch("2_2_R2.fq")
          )
        ))))
}
