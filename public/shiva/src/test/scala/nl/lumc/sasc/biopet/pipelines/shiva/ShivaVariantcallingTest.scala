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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.{ File, FileOutputStream }

import com.google.common.io.Files
import nl.lumc.sasc.biopet.utils.config.Config
import nl.lumc.sasc.biopet.extensions.Freebayes
import nl.lumc.sasc.biopet.extensions.gatk.CombineVariants
import nl.lumc.sasc.biopet.extensions.tools.VcfFilter
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ AfterClass, DataProvider, Test }

import scala.collection.mutable.ListBuffer

/**
 * Test class for [[ShivaVariantcalling]]
 *
 * Created by pjvan_thof on 3/2/15.
 */
class ShivaVariantcallingTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): ShivaVariantcalling = {
    new ShivaVariantcalling {
      override def configNamespace = "shivavariantcalling"
      override def globalConfig = new Config(ConfigUtils.mergeMaps(map, ShivaVariantcallingTest.config))
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "shivaVariantcallingOptions")
  def shivaVariantcallingOptions = {
    val bool = Array(true, false)
    (for (
      bams <- 0 to 3;
      raw <- bool;
      bcftools <- bool;
      bcftoolsSinglesample <- bool;
      freebayes <- bool;
      varscanCnsSinglesample <- bool
    ) yield Array(bams, raw, bcftools, bcftoolsSinglesample, freebayes, varscanCnsSinglesample)).toArray
  }

  @Test(dataProvider = "shivaVariantcallingOptions")
  def testShivaVariantcalling(bams: Int,
                              raw: Boolean,
                              bcftools: Boolean,
                              bcftoolsSinglesample: Boolean,
                              freebayes: Boolean,
                              varscanCnsSinglesample: Boolean) = {
    val callers: ListBuffer[String] = ListBuffer()
    if (raw) callers.append("raw")
    if (bcftools) callers.append("bcftools")
    if (bcftoolsSinglesample) callers.append("bcftools_singlesample")
    if (freebayes) callers.append("freebayes")
    if (varscanCnsSinglesample) callers.append("varscan_cns_singlesample")
    val map = Map("variantcallers" -> callers.toList)
    val pipeline = initPipeline(map)

    pipeline.inputBams = (for (n <- 1 to bams) yield n.toString -> ShivaVariantcallingTest.inputTouch("bam_" + n + ".bam")).toMap

    val illegalArgumentException = pipeline.inputBams.isEmpty || (!raw && !bcftools && !bcftoolsSinglesample && !freebayes && !varscanCnsSinglesample)

    if (illegalArgumentException) intercept[IllegalArgumentException] {
      pipeline.init()
      pipeline.script()
    }

    if (!illegalArgumentException) {
      pipeline.init()
      pipeline.script()

      pipeline.functions.count(_.isInstanceOf[CombineVariants]) shouldBe (1 + (if (raw) 1 else 0) + (if (varscanCnsSinglesample) 1 else 0))
      //pipeline.functions.count(_.isInstanceOf[Bcftools]) shouldBe (if (bcftools) 1 else 0)
      //FIXME: Can not check for bcftools because of piping
      pipeline.functions.count(_.isInstanceOf[Freebayes]) shouldBe (if (freebayes) 1 else 0)
      //pipeline.functions.count(_.isInstanceOf[MpileupToVcf]) shouldBe (if (raw) bams else 0)
      pipeline.functions.count(_.isInstanceOf[VcfFilter]) shouldBe (if (raw) bams else 0)
    }
  }
}

object ShivaVariantcallingTest {
  val outputDir = Files.createTempDir()
  outputDir.deleteOnExit()
  new File(outputDir, "input").mkdirs()
  private def inputTouch(name: String): File = {
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
    "reference_fasta" -> (outputDir + File.separator + "ref.fa"),
    "gatk_jar" -> "test",
    "samtools" -> Map("exe" -> "test"),
    "bcftools" -> Map("exe" -> "test"),
    "freebayes" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test"),
    "bgzip" -> Map("exe" -> "test"),
    "tabix" -> Map("exe" -> "test"),
    "rscript" -> Map("exe" -> "test"),
    "exe" -> "test",
    "varscan_jar" -> "test"
  )
}