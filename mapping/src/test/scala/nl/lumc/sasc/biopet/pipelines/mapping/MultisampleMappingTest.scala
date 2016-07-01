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
package nl.lumc.sasc.biopet.pipelines.mapping

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions.kraken.Kraken
import nl.lumc.sasc.biopet.extensions.picard.{MarkDuplicates, MergeSamFiles}
import nl.lumc.sasc.biopet.utils.{ConfigUtils, Logging}
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{DataProvider, Test}

/**
 * Created by pjvanthof on 15/05/16.
 */
trait MultisampleMappingTestTrait extends TestNGSuite with Matchers {
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
  def unmappedToGears = false
  def sample1 = Array(true, false)
  def sample2 = Array(true, false)
  def sample3 = false
  def sample4 = false

  @DataProvider(name = "mappingOptions")
  def mappingOptions = {
    for (
      merge <- mergeStrategies.toArray; s1 <- sample1; s2 <- sample2
    ) yield Array(merge, s1, s2)
  }

  @Test(dataProvider = "mappingOptions")
  def testMultisampleMapping(merge: MultisampleMapping.MergeStrategy.Value, sample1: Boolean, sample2: Boolean): Unit = {
    val map: Map[String, Any] = {
      var m: Map[String, Any] = MultisampleMappingTestTrait.config
      if (sample1) m = ConfigUtils.mergeMaps(MultisampleMappingTestTrait.sample1, m)
      if (sample2) m = ConfigUtils.mergeMaps(MultisampleMappingTestTrait.sample2, m)
      if (sample3) m = ConfigUtils.mergeMaps(MultisampleMappingTestTrait.sample3, m)
      if (sample4) m = ConfigUtils.mergeMaps(MultisampleMappingTestTrait.sample4, m)
      m ++ Map(
        "merge_strategy" -> merge.toString,
        "bam_to_fastq" -> bamToFastq,
        "correct_readgroups" -> correctReadgroups,
        "unmapped_to_gears" -> unmappedToGears
      )
    }

    if (!sample1 && !sample2 && !sample3 && !sample4) { // When no samples
      intercept[IllegalStateException] {
        initPipeline(map).script()
      }
      Logging.errors.clear()
    } else if (sample4 && !bamToFastq && !correctReadgroups) {
      intercept[IllegalStateException] {
        initPipeline(map).script()
      }
    } else {
      val pipeline = initPipeline(map)
      pipeline.script()

      val numberFastqLibs = (if (sample1) 1 else 0) + (if (sample2) 2 else 0) + (if (sample3 && bamToFastq) 1 else 0) + (if (sample4 && bamToFastq) 1 else 0)
      val numberSamples = (if (sample1) 1 else 0) + (if (sample2) 1 else 0)

      import MultisampleMapping.MergeStrategy
      pipeline.functions.count(_.isInstanceOf[MarkDuplicates]) shouldBe (numberFastqLibs +
        (if (sample2 && (merge == MergeStrategy.MarkDuplicates || merge == MergeStrategy.PreProcessMarkDuplicates)) 1 else 0))
      pipeline.functions.count(_.isInstanceOf[MergeSamFiles]) shouldBe (
        (if (sample2 && (merge == MergeStrategy.MergeSam || merge == MergeStrategy.PreProcessMergeSam)) 1 else 0))
      pipeline.samples.foreach {
        case (sampleName, sample) =>
          if (merge == MergeStrategy.None) sample.bamFile shouldBe None
          sample.summaryStats shouldBe Map()
          sample.libraries.foreach {
            case (libraryId, library) =>
              library.summaryStats shouldBe Map()
          }
      }

      pipeline.functions.count(_.isInstanceOf[Kraken]) shouldBe (if (unmappedToGears) (numberFastqLibs + numberSamples) else 0)

      pipeline.summarySettings.get("merge_strategy") shouldBe Some(merge.toString)
    }
  }
}

class MultisampleMappingTest extends MultisampleMappingTestTrait {
  override def sample1 = Array(true)
}

class MultisampleMappingNoSamplesTest extends MultisampleMappingTestTrait {
  override def sample1 = Array(false)
  override def sample2 = Array(false)
  override def mergeStrategies = MultisampleMapping.MergeStrategy.values.filter(_ == MultisampleMapping.MergeStrategy.PreProcessMarkDuplicates)
}

class MultisampleMappingGearsTest extends MultisampleMappingTestTrait {
  override def sample1 = Array(true)
  override def sample2 = Array(false)
  override def unmappedToGears = true
  override def mergeStrategies = MultisampleMapping.MergeStrategy.values.filter(_ == MultisampleMapping.MergeStrategy.PreProcessMarkDuplicates)
}

class MultisampleMappingBamTest extends MultisampleMappingTestTrait {
  override def sample1 = Array(false)
  override def sample2 = Array(false)
  override def sample3 = true
  override def mergeStrategies = MultisampleMapping.MergeStrategy.values.filter(_ == MultisampleMapping.MergeStrategy.PreProcessMarkDuplicates)
}

class MultisampleMappingWrongBamTest extends MultisampleMappingTestTrait {
  override def sample1 = Array(false)
  override def sample2 = Array(false)
  override def sample4 = true
  override def mergeStrategies = MultisampleMapping.MergeStrategy.values.filter(_ == MultisampleMapping.MergeStrategy.PreProcessMarkDuplicates)
}

class MultisampleMappingCorrectBamTest extends MultisampleMappingTestTrait {
  override def sample1 = Array(false)
  override def sample2 = Array(false)
  override def correctReadgroups = true
  override def sample4 = true
  override def mergeStrategies = MultisampleMapping.MergeStrategy.values.filter(_ == MultisampleMapping.MergeStrategy.PreProcessMarkDuplicates)
}

class MultisampleMappingBamToFastqTest extends MultisampleMappingTestTrait {
  override def sample1 = Array(false)
  override def sample2 = Array(false)
  override def bamToFastq = true
  override def sample3 = true
  override def sample4 = true
  override def mergeStrategies = MultisampleMapping.MergeStrategy.values.filter(_ == MultisampleMapping.MergeStrategy.PreProcessMarkDuplicates)
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
  copyFile("empty.sam")

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
    "kraken" -> Map("exe" -> "test", "db" -> "test"),
    "krakenreport" -> Map("exe" -> "test", "db" -> "test"),
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

  val sample3 = Map(
    "samples" -> Map("sample3" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "bam" -> (outputDir + File.separator + "empty.sam")
      )
    )
    )))

  val sample4 = Map(
    "samples" -> Map("sample4" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "bam" -> (outputDir + File.separator + "empty.sam")
      )
    )
    )))
}
