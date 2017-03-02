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

import java.io.{ File, FileOutputStream }

import com.google.common.io.Files
import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.extensions.centrifuge.Centrifuge
import nl.lumc.sasc.biopet.extensions.picard.{ MarkDuplicates, MergeSamFiles }
import nl.lumc.sasc.biopet.utils.{ ConfigUtils, Logging }
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ AfterClass, DataProvider, Test }

/**
 * Created by pjvanthof on 15/05/16.
 */
trait MultisampleMappingTestTrait extends TestNGSuite with Matchers {

  val outputDir = MultisampleMappingTestTrait.outputDir
  val configMap = MultisampleMappingTestTrait.config(outputDir)

  def initPipeline(map: Map[String, Any]): MultisampleMapping = {
    new MultisampleMapping() {
      override def configNamespace = "multisamplemapping"
      override def globalConfig = new Config(ConfigUtils.mergeMaps(map, configMap))
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
      var m: Map[String, Any] = configMap
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

      val pipesJobs = pipeline.functions.filter(_.isInstanceOf[BiopetCommandLineFunction])
        .flatMap(_.asInstanceOf[BiopetCommandLineFunction].pipesJobs)

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

      pipesJobs.count(_.isInstanceOf[Centrifuge]) shouldBe (if (unmappedToGears) (numberFastqLibs + numberSamples) else 0)

      pipeline.summarySettings.get("merge_strategy") shouldBe Some(merge.toString)
    }
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    FileUtils.deleteDirectory(outputDir)
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
  def outputDir = Files.createTempDir()

  val inputDir = Files.createTempDir()

  def inputTouch(name: String): File = {
    val file = new File(inputDir, name).getAbsoluteFile
    Files.touch(file)
    file
  }

  private def copyFile(name: String): Unit = {
    val is = getClass.getResourceAsStream("/" + name)
    val out = new File(inputDir, name)
    out.deleteOnExit()
    val os = new FileOutputStream(out)
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  copyFile("ref.fa")
  copyFile("ref.dict")
  copyFile("ref.fa.fai")
  copyFile("empty.sam")

  def config(outputDir: File) = Map(
    "skip_write_dependencies" -> true,
    "name_prefix" -> "test",
    "cache" -> true,
    "dir" -> "test",
    "vep_script" -> "test",
    "output_dir" -> outputDir,
    "reference_fasta" -> (inputDir + File.separator + "ref.fa"),
    "fastqc" -> Map("exe" -> "test"),
    "input_alleles" -> "test",
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "sickle" -> Map("exe" -> "test"),
    "cutadapt" -> Map("exe" -> "test"),
    "bwa" -> Map("exe" -> "test"),
    "samtools" -> Map("exe" -> "test"),
    "igvtools" -> Map("exe" -> "test", "igvtools_jar" -> "test"),
    "wigtobigwig" -> Map("exe" -> "test"),
    "kraken" -> Map("exe" -> "test", "db" -> "test"),
    "krakenreport" -> Map("exe" -> "test", "db" -> "test"),
    "centrifuge" -> Map("exe" -> "test"),
    "centrifugekreport" -> Map("exe" -> "test"),
    "centrifuge_index" -> "test",
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
        "bam" -> (inputDir + File.separator + "empty.sam")
      )
    )
    )))

  val sample4 = Map(
    "samples" -> Map("sample4" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "bam" -> (inputDir + File.separator + "empty.sam")
      )
    )
    )))
}
