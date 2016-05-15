package nl.lumc.sasc.biopet.pipelines.mapping

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions.picard.{MarkDuplicates, MergeSamFiles}
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{DataProvider, Test}

/**
  * Created by pjvanthof on 15/05/16.
  */
class MultisampleMappingTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): MultisampleMapping = {
    new MultisampleMapping() {
      override def configNamespace = "multisamplemapping"
      override def globalConfig = new Config(ConfigUtils.mergeMaps(map, MultisampleMappingTestTrait.config))
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  def mergeStrategies = MultisampleMapping.MergeStrategy.values
  def bamToFastq = false
  def correctReadgroups = false

  @DataProvider(name = "mappingOptions")
  def mappingOptions = {
    val bool = Array(true, false)
    for (
      merge <- mergeStrategies.toArray; sample2 <- bool
    ) yield Array(merge, true, sample2)
  }

  @Test(dataProvider = "mappingOptions")
  def testMultisampleMapping(merge: MultisampleMapping.MergeStrategy.Value, sample1: Boolean, sample2: Boolean): Unit = {
    val map: Map[String, Any] = {
      var m: Map[String, Any] = MultisampleMappingTestTrait.config
      if (sample1) m = ConfigUtils.mergeMaps(MultisampleMappingTestTrait.sample1, m)
      if (sample2) m = ConfigUtils.mergeMaps(MultisampleMappingTestTrait.sample2, m)
      m ++ Map("merge_strategy" -> merge.toString, "bam_to_fastq" -> bamToFastq, "correct_readgroups" -> correctReadgroups)
    }

    if (!sample1 && !sample2) { // When no samples
      intercept[IllegalArgumentException] {
        initPipeline(map).script()
      }
    } else {
      val pipeline = initPipeline(map)
      pipeline.script()

      val numberLibs = (if (sample1) 1 else 0) + (if (sample2) 2 else 0)
      val numberSamples = (if (sample1) 1 else 0) + (if (sample2) 1 else 0)

      import MultisampleMapping.MergeStrategy
      pipeline.functions.count(_.isInstanceOf[MarkDuplicates]) shouldBe (numberLibs +
        (if (sample2 && (merge == MergeStrategy.MarkDuplicates || merge == MergeStrategy.PreProcessMarkDuplicates)) 1 else 0))
      pipeline.functions.count(_.isInstanceOf[MergeSamFiles]) shouldBe (
        (if (sample2 && (merge == MergeStrategy.MergeSam || merge == MergeStrategy.PreProcessMergeSam)) 1 else 0))
      pipeline.samples.foreach { case (sampleName, sample) =>
        if (merge == MergeStrategy.None) sample.bamFile shouldBe None
      }
    }
  }
}

object MultisampleMappingTestTrait {
  val outputDir = Files.createTempDir()
  outputDir.deleteOnExit()
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
    "cache" -> true,
    "dir" -> "test",
    "vep_script" -> "test",
    "output_dir" -> outputDir,
    "reference_fasta" -> (outputDir + File.separator + "ref.fa"),
    "fastqc" -> Map("exe" -> "test"),
    "input_alleles" -> "test",
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "sickle" -> Map("exe" -> "test"),
    "cutadapt" -> Map("exe" -> "test"),
    "bwa" -> Map("exe" -> "test"),
    "samtools" -> Map("exe" -> "test"),
    "igvtools" -> Map("exe" -> "test"),
    "wigtobigwig" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test")
  )

  val sample1 = Map(
    "samples" -> Map("sample1" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "R1" -> inputTouch("1_1_R1.fq"),
        "R2" -> inputTouch("1_1_R2.fq")
      )
    )
    )))

  val sample2 = Map(
    "samples" -> Map("sample3" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "R1" -> inputTouch("2_1_R1.fq"),
        "R2" -> inputTouch("2_1_R2.fq")
      ),
      "lib2" -> Map(
        "R1" -> inputTouch("2_2_R1.fq"),
        "R2" -> inputTouch("2_2_R2.fq")
      )
    )
    )))
}
