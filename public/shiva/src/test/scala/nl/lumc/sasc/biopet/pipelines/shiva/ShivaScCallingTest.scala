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
import nl.lumc.sasc.biopet.extensions.breakdancer.{ BreakdancerVCF, BreakdancerConfig, BreakdancerCaller }
import nl.lumc.sasc.biopet.extensions.clever.CleverCaller
import nl.lumc.sasc.biopet.extensions.delly.DellyCaller
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
class ShivaSvCallingTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): ShivaSvCalling = {
    new ShivaSvCalling {
      override def configName = "shivasvcalling"
      override def globalConfig = new Config(ConfigUtils.mergeMaps(map, ShivaSvCallingTest.config))
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "shivaSvCallingOptions")
  def shivaSvCallingOptions = {
    val bool = Array(true, false)
    (for (
      bams <- 0 to 3;
      delly <- bool;
      clever <- bool;
      breakdancer <- bool
    ) yield Array(bams, delly, clever, breakdancer)).toArray
  }

  @Test(dataProvider = "shivaSvCallingOptions")
  def testShivaSvCcalling(bams: Int,
                          delly: Boolean,
                          clever: Boolean,
                          breakdancer: Boolean) = {
    val callers: ListBuffer[String] = ListBuffer()
    if (delly) callers.append("delly")
    if (clever) callers.append("clever")
    if (breakdancer) callers.append("breakdancer")
    val map = Map("sv_callers" -> callers.toList)
    val pipeline = initPipeline(map)

    pipeline.inputBams = (for (n <- 1 to bams) yield n.toString -> ShivaSvCallingTest.inputTouch("bam_" + n + ".bam")).toMap

    val illegalArgumentException = pipeline.inputBams.isEmpty || (!delly && !clever && !breakdancer)

    if (illegalArgumentException) intercept[IllegalArgumentException] {
      pipeline.init()
      pipeline.script()
    }

    if (!illegalArgumentException) {
      pipeline.init()
      pipeline.script()

      pipeline.functions.count(_.isInstanceOf[BreakdancerCaller]) shouldBe (if (breakdancer) bams else 0)
      pipeline.functions.count(_.isInstanceOf[BreakdancerConfig]) shouldBe (if (breakdancer) bams else 0)
      pipeline.functions.count(_.isInstanceOf[BreakdancerVCF]) shouldBe (if (breakdancer) bams else 0)
      pipeline.functions.count(_.isInstanceOf[CleverCaller]) shouldBe (if (clever) bams else 0)
      pipeline.functions.count(_.isInstanceOf[DellyCaller]) shouldBe (if (delly) (bams * 4) else 0)

    }
  }

  @AfterClass def removeTempOutputDir() = {
    FileUtils.deleteDirectory(ShivaSvCallingTest.outputDir)
  }
}

object ShivaSvCallingTest {
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
    "reference_fasta" -> (outputDir + File.separator + "ref.fa"),
    "gatk_jar" -> "test",
    "samtools" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test"),
    "bgzip" -> Map("exe" -> "test"),
    "tabix" -> Map("exe" -> "test"),
    "breakdancerconfig" -> Map("exe" -> "test"),
    "breakdancercaller" -> Map("exe" -> "test"),
    "clever" -> Map("exe" -> "test"),
    "delly" -> Map("exe" -> "test"),
    "varscan_jar" -> "test"
  )
}