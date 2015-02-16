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
import org.testng.annotations.{ DataProvider, Test }

/**
 * Created by pjvan_thof on 2/11/15.
 */
class FlexiprepTest extends TestNGSuite with Matchers {

  def initPipeline(map: Map[String, Any]): Flexiprep = {
    new Flexiprep() {
      override def configName = "flexiprep"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "flexiprepOptions", parallel = true)
  def flexiprepOptions = {
    val paired = Array(true, false)
    val skipTrims = Array(true, false)
    val skipClips = Array(true, false)
    val zipped = Array(true, false)

    for (
      pair <- paired;
      skipTrim <- skipTrims;
      skipClip <- skipClips;
      zip <- zipped
    ) yield Array("", pair, skipTrim, skipClip, zip)
  }

  @Test(dataProvider = "flexiprepOptions")
  def testFlexiprep(f: String, paired: Boolean, skipTrim: Boolean, skipClip: Boolean, zipped: Boolean) = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> FlexiprepTest.outputDir,
      "skip_trim" -> skipTrim,
      "skip_clip" -> skipClip
    ), Map(FlexiprepTest.excutables.toSeq: _*))
    val flexiprep: Flexiprep = initPipeline(map)

    flexiprep.input_R1 = new File(flexiprep.outputDir, "bla_R1.fq" + (if (zipped) ".gz" else ""))
    if (paired) flexiprep.input_R2 = Some(new File(flexiprep.outputDir, "bla_R2.fq" + (if (zipped) ".gz" else "")))
    flexiprep.sampleId = Some("1")
    flexiprep.libId = Some("1")
    flexiprep.script()

    flexiprep.functions.count(_.isInstanceOf[Fastqc]) shouldBe (
      if (paired && (skipClip && skipTrim)) 2
      else if (!paired && (skipClip && skipTrim)) 1
      else if (paired && !(skipClip && skipTrim)) 4
      else if (!paired && !(skipClip && skipTrim)) 2)
    flexiprep.functions.count(_.isInstanceOf[Zcat]) shouldBe (if (zipped) (if (paired) 2 else 1) else 0)
    flexiprep.functions.count(_.isInstanceOf[SeqtkSeq]) shouldBe (if (paired) 2 else 1)
    flexiprep.functions.count(_.isInstanceOf[Cutadapt]) shouldBe (if (skipClip) 0 else (if (paired) 2 else 1))
    flexiprep.functions.count(_.isInstanceOf[FastqSync]) shouldBe (if (skipClip) 0 else (if (paired) 1 else 0))
    flexiprep.functions.count(_.isInstanceOf[Sickle]) shouldBe (if (skipTrim) 0 else 1)
    flexiprep.functions.count(_.isInstanceOf[Gzip]) shouldBe (if (paired) 2 else 1)
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