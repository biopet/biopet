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

import java.io.{ File, FileOutputStream }
import java.nio.file.Paths

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions.breakdancer.{ BreakdancerCaller, BreakdancerConfig, BreakdancerVCF }
import nl.lumc.sasc.biopet.extensions.clever.CleverCaller
import nl.lumc.sasc.biopet.extensions.delly.DellyCaller
import nl.lumc.sasc.biopet.extensions.pindel.{ PindelCaller, PindelConfig, PindelVCF }
import nl.lumc.sasc.biopet.utils.{ ConfigUtils, Logging }
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ DataProvider, Test }

import scala.collection.mutable.ListBuffer

/**
 * Test class for [[ShivaVariantcalling]]
 *
 * Created by pjvan_thof on 3/2/15.
 */
class ShivaSvCallingTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): ShivaSvCalling = {
    new ShivaSvCalling {
      override def configNamespace = "shivasvcalling"
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
      breakdancer <- bool;
      pindel <- bool
    ) yield Array(bams, delly, clever, breakdancer, pindel)).toArray
  }

  @Test(dataProvider = "shivaSvCallingOptions")
  def testShivaSvCalling(bams: Int,
                         delly: Boolean,
                         clever: Boolean,
                         breakdancer: Boolean,
                         pindel: Boolean) = {
    val callers: ListBuffer[String] = ListBuffer()
    if (delly) callers.append("delly")
    if (clever) callers.append("clever")
    if (breakdancer) callers.append("breakdancer")
    if (pindel) callers.append("pindel")
    val map = Map("sv_callers" -> callers.toList)
    val pipeline = initPipeline(map)

    pipeline.inputBams = (for (n <- 1 to bams) yield n.toString -> ShivaSvCallingTest.inputTouch("bam_" + n + ".bam")).toMap

    val illegalArgumentException = pipeline.inputBams.isEmpty || (!delly && !clever && !breakdancer && !pindel)

    if (illegalArgumentException) intercept[IllegalArgumentException] {
      pipeline.init()
      pipeline.script()
    }

    if (!illegalArgumentException) {
      pipeline.init()
      pipeline.script()

      val summaryCallers = pipeline.summarySettings("sv_callers")
      if (delly) assert(summaryCallers.contains("delly"))
      else assert(!summaryCallers.contains("delly"))
      if (clever) assert(summaryCallers.contains("clever"))
      else assert(!summaryCallers.contains("clever"))
      if (breakdancer) assert(summaryCallers.contains("breakdancer"))
      else assert(!summaryCallers.contains("breakdancer"))
      if (pindel) assert(summaryCallers.contains("pindel"))
      else assert(!summaryCallers.contains("pindel"))

      pipeline.functions.count(_.isInstanceOf[BreakdancerConfig]) shouldBe (if (breakdancer) bams else 0)
      pipeline.functions.count(_.isInstanceOf[BreakdancerCaller]) shouldBe (if (breakdancer) bams else 0)
      pipeline.functions.count(_.isInstanceOf[BreakdancerVCF]) shouldBe (if (breakdancer) bams else 0)

      pipeline.functions.count(_.isInstanceOf[PindelConfig]) shouldBe (if (pindel) bams else 0)
      pipeline.functions.count(_.isInstanceOf[PindelCaller]) shouldBe (if (pindel) bams else 0)
      pipeline.functions.count(_.isInstanceOf[PindelVCF]) shouldBe (if (pindel) bams else 0)

      pipeline.functions.count(_.isInstanceOf[CleverCaller]) shouldBe (if (clever) bams else 0)
      pipeline.functions.count(_.isInstanceOf[DellyCaller]) shouldBe (if (delly) bams * 4 else 0)

    }
  }

  @DataProvider(name = "dellyOptions")
  def dellyOptions = {
    val bool = Array(true, false)
    for (
      del <- bool;
      dup <- bool;
      inv <- bool;
      tra <- bool
    ) yield Array(1, del, dup, inv, tra)
  }

  @Test(dataProvider = "dellyOptions")
  def testShivaDelly(bams: Int, del: Boolean, dup: Boolean, inv: Boolean, tra: Boolean): Unit = {

    val map = Map("sv_callers" -> List("delly"), "delly" ->
      Map("DEL" -> del, "DUP" -> dup, "INV" -> inv, "TRA" -> tra)
    )
    val pipeline = initPipeline(map)

    pipeline.inputBams = Map("bam" -> ShivaSvCallingTest.inputTouch("bam" + ".bam"))

    if (!del && !dup && !inv && !tra) intercept[IllegalStateException] {
      pipeline.init()
      pipeline.script()
    }
    else {
      pipeline.init()
      pipeline.script()

      pipeline.functions.count(_.isInstanceOf[DellyCaller]) shouldBe
        ((if (del) 1 else 0) + (if (dup) 1 else 0) + (if (inv) 1 else 0) + (if (tra) 1 else 0))
    }
  }

  @Test
  def testWrongCaller(): Unit = {
    val map = Map("sv_callers" -> List("this is not a caller"))
    val pipeline = initPipeline(map)

    pipeline.inputBams = Map("bam" -> ShivaSvCallingTest.inputTouch("bam" + ".bam"))

    intercept[IllegalArgumentException] {
      pipeline.script()
    }
    Logging.errors.clear()
  }

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testInputBamsArg(): Unit = {
    val pipeline = initPipeline(Map())

    pipeline.inputBamsArg :+= new File(resourcePath("/paired01.bam"))

    pipeline.init()
    pipeline.script()

    val summaryCallers = pipeline.summarySettings("sv_callers")
    assert(summaryCallers.contains("delly"))
    assert(summaryCallers.contains("clever"))
    assert(summaryCallers.contains("breakdancer"))
  }
}

object ShivaSvCallingTest {
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
    "varscan_jar" -> "test",
    "pysvtools" -> Map(
      "exe" -> "test",
      "exclusion_regions" -> "test",
      "translocations_only" -> false)
  )
}