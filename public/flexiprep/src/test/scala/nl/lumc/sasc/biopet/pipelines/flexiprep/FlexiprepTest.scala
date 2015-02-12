package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.File

import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.extensions.{ Gzip, Zcat }
import nl.lumc.sasc.biopet.tools.FastqSync
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.QSettings
import org.broadinstitute.gatk.queue.function.QFunction
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 2/11/15.
 */
class FlexiprepTest extends TestNGSuite with Matchers {

  def initPipeline(map: Map[String, Any]): Flexiprep = {
    new Flexiprep() {
      override def configName = "flexiprep"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test." + System.currentTimeMillis
    }
  }

  @Test def TestDefault = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> FlexiprepTest.outputDir), Map(FlexiprepTest.excutables.toSeq: _*))
    val flexiprep: Flexiprep = initPipeline(map)

    flexiprep.input_R1 = new File(flexiprep.outputDir, "bla.fq")
    flexiprep.sampleId = "1"
    flexiprep.libId = "1"
    flexiprep.script()

    flexiprep.functions.count(_.isInstanceOf[Fastqc]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[Zcat]) shouldBe 0
    flexiprep.functions.count(_.isInstanceOf[SeqtkSeq]) shouldBe 1
    flexiprep.functions.count(_.isInstanceOf[Cutadapt]) shouldBe 1
    flexiprep.functions.count(_.isInstanceOf[FastqSync]) shouldBe 0
    flexiprep.functions.count(_.isInstanceOf[Sickle]) shouldBe 1
    flexiprep.functions.count(_.isInstanceOf[Gzip]) shouldBe 1
  }

  @Test def TestDefaultPaired = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> FlexiprepTest.outputDir), Map(FlexiprepTest.excutables.toSeq: _*))
    val flexiprep: Flexiprep = initPipeline(map)

    flexiprep.input_R1 = new File(flexiprep.outputDir, "bla_R1.fq.gz")
    flexiprep.input_R2 = Some(new File(flexiprep.outputDir, "bla_R2.fq.gz"))
    flexiprep.sampleId = "1"
    flexiprep.libId = "1"
    flexiprep.script()

    flexiprep.functions.count(_.isInstanceOf[Fastqc]) shouldBe 4
    flexiprep.functions.count(_.isInstanceOf[Zcat]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[SeqtkSeq]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[Cutadapt]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[FastqSync]) shouldBe 1
    flexiprep.functions.count(_.isInstanceOf[Sickle]) shouldBe 1
    flexiprep.functions.count(_.isInstanceOf[Gzip]) shouldBe 2
  }

  @Test def TestClipTrimPaired = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> FlexiprepTest.outputDir, "skip_trim" -> false, "skip_clip" -> false),
      Map(FlexiprepTest.excutables.toSeq: _*))
    val flexiprep: Flexiprep = initPipeline(map)

    flexiprep.input_R1 = new File(flexiprep.outputDir, "bla_R1.fq.gz")
    flexiprep.input_R2 = Some(new File(flexiprep.outputDir, "bla_R2.fq.gz"))
    flexiprep.sampleId = "1"
    flexiprep.libId = "1"
    flexiprep.script()

    flexiprep.functions.count(_.isInstanceOf[Fastqc]) shouldBe 4
    flexiprep.functions.count(_.isInstanceOf[Zcat]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[SeqtkSeq]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[Cutadapt]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[FastqSync]) shouldBe 1
    flexiprep.functions.count(_.isInstanceOf[Sickle]) shouldBe 1
    flexiprep.functions.count(_.isInstanceOf[Gzip]) shouldBe 2
  }

  @Test def TestTrimPaired = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> FlexiprepTest.outputDir, "skip_trim" -> false, "skip_clip" -> true),
      Map(FlexiprepTest.excutables.toSeq: _*))
    val flexiprep: Flexiprep = initPipeline(map)

    flexiprep.input_R1 = new File(flexiprep.outputDir, "bla_R1.fq.gz")
    flexiprep.input_R2 = Some(new File(flexiprep.outputDir, "bla_R2.fq.gz"))
    flexiprep.sampleId = "1"
    flexiprep.libId = "1"
    flexiprep.script()

    flexiprep.functions.count(_.isInstanceOf[Fastqc]) shouldBe 4
    flexiprep.functions.count(_.isInstanceOf[Zcat]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[SeqtkSeq]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[Cutadapt]) shouldBe 0
    flexiprep.functions.count(_.isInstanceOf[FastqSync]) shouldBe 0
    flexiprep.functions.count(_.isInstanceOf[Sickle]) shouldBe 1
    flexiprep.functions.count(_.isInstanceOf[Gzip]) shouldBe 2
  }

  @Test def TestClipPaired = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> FlexiprepTest.outputDir, "skip_trim" -> true, "skip_clip" -> false),
      Map(FlexiprepTest.excutables.toSeq: _*))
    val flexiprep: Flexiprep = initPipeline(map)

    flexiprep.input_R1 = new File(flexiprep.outputDir, "bla_R1.fq.gz")
    flexiprep.input_R2 = Some(new File(flexiprep.outputDir, "bla_R2.fq.gz"))
    flexiprep.sampleId = "1"
    flexiprep.libId = "1"
    flexiprep.script()

    flexiprep.functions.count(_.isInstanceOf[Fastqc]) shouldBe 4
    flexiprep.functions.count(_.isInstanceOf[Zcat]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[SeqtkSeq]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[Cutadapt]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[FastqSync]) shouldBe 1
    flexiprep.functions.count(_.isInstanceOf[Sickle]) shouldBe 0
    flexiprep.functions.count(_.isInstanceOf[Gzip]) shouldBe 2
  }

  @Test def TestPaired = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> FlexiprepTest.outputDir, "skip_trim" -> true, "skip_clip" -> true),
      Map(FlexiprepTest.excutables.toSeq: _*))
    val flexiprep: Flexiprep = initPipeline(map)

    flexiprep.input_R1 = new File(flexiprep.outputDir, "bla_R1.fq.gz")
    flexiprep.input_R2 = Some(new File(flexiprep.outputDir, "bla_R2.fq.gz"))
    flexiprep.sampleId = "1"
    flexiprep.libId = "1"
    flexiprep.script()

    flexiprep.functions.count(_.isInstanceOf[Fastqc]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[Zcat]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[SeqtkSeq]) shouldBe 2
    flexiprep.functions.count(_.isInstanceOf[Cutadapt]) shouldBe 0
    flexiprep.functions.count(_.isInstanceOf[FastqSync]) shouldBe 0
    flexiprep.functions.count(_.isInstanceOf[Sickle]) shouldBe 0
    flexiprep.functions.count(_.isInstanceOf[Gzip]) shouldBe 2
  }
}
object FlexiprepTest {
  val outputDir = System.getProperty("java.io.tmpdir") + File.separator + "flexiprep"

  val excutables = Map(
    "seqstat" -> Map("exe" -> "test"),
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "sickle" -> Map("exe" -> "test")
  )
}