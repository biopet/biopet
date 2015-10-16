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
package nl.lumc.sasc.biopet.pipelines.mapping

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.pipelines.flexiprep.Fastqc
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{AfterClass, DataProvider, Test}

/**
 * Test class for [[Mapping]]
 *
 * Created by pjvan_thof on 2/12/15.
 */
abstract class AbstractTestMapping(val aligner: String) extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): Mapping = {
    new Mapping {
      override def configName = "mapping"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "mappingOptions")
  def mappingOptions = {
    val paired = Array(true, false)
    val chunks = Array(1, 5)
    val skipMarkDuplicates = Array(true, false)
    val skipFlexipreps = Array(true, false)
    val zipped = Array(true, false)

    for (
      pair <- paired;
      chunk <- chunks;
      skipMarkDuplicate <- skipMarkDuplicates;
      skipFlexiprep <- skipFlexipreps;
      zipped <- zipped
    ) yield Array(aligner, pair, chunk, skipMarkDuplicate, skipFlexiprep, zipped)
  }

  @Test(dataProvider = "mappingOptions")
  def testMapping(aligner: String, paired: Boolean, chunks: Int,
                  skipMarkDuplicate: Boolean,
                  skipFlexiprep: Boolean,
                  zipped: Boolean) = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> MappingTest.outputDir,
      "aligner" -> aligner,
      "number_chunks" -> chunks,
      "skip_markduplicates" -> skipMarkDuplicate,
      "skip_flexiprep" -> skipFlexiprep
    ), Map(MappingTest.executables.toSeq: _*))
    val mapping: Mapping = initPipeline(map)

    if (zipped) {
      mapping.input_R1 = MappingTest.r1Zipped
      if (paired) mapping.input_R2 = Some(MappingTest.r2Zipped)
    } else {
      mapping.input_R1 = MappingTest.r1
      if (paired) mapping.input_R2 = Some(MappingTest.r2)
    }
    mapping.sampleId = Some("1")
    mapping.libId = Some("1")
    mapping.script()

    //Flexiprep
    mapping.functions.count(_.isInstanceOf[Fastqc]) shouldBe (if (skipFlexiprep) 0 else if (paired) 4 else 2)
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    FileUtils.deleteDirectory(MappingTest.outputDir)
  }
}

class MappingBwaMemTest extends AbstractTestMapping("bwa-mem")
class MappingBwaAlnTest extends AbstractTestMapping("bwa-aln")
class MappingStarTest extends AbstractTestMapping("star")
class MappingStar2PassTest extends AbstractTestMapping("star-2pass")
class MappingBowtieTest extends AbstractTestMapping("bowtie")
class MappingStampyTest extends AbstractTestMapping("stampy")
class MappingGsnapTest extends AbstractTestMapping("gsnap")
class MappingTophatTest extends AbstractTestMapping("tophat")

object MappingTest {

  val outputDir = Files.createTempDir()
  new File(outputDir, "input").mkdirs()

  val r1 = new File(outputDir, "input" + File.separator + "R1.fq")
  Files.touch(r1)
  val r2 = new File(outputDir, "input" + File.separator + "R2.fq")
  Files.touch(r2)
  val r1Zipped = new File(outputDir, "input" + File.separator + "R1.fq.gz")
  Files.touch(r1Zipped)
  val r2Zipped = new File(outputDir, "input" + File.separator + "R2.fq.gz")
  Files.touch(r2Zipped)

  private def copyFile(name: String): Unit = {
    val is = getClass.getResourceAsStream("/" + name)
    val os = new FileOutputStream(new File(outputDir, name))
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  copyFile("ref.fa")
  copyFile("ref.dict")
  copyFile("ref.fa.fai")
  copyFile("ref.1.bt2")
  copyFile("ref.1.ebwt")

  val executables = Map(
    "reference_fasta" -> (outputDir + File.separator + "ref.fa"),
    "db" -> "test",
    "bowtie_index" -> (outputDir + File.separator + "ref"),
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "gsnap" -> Map("exe" -> "test"),
    "tophat" -> Map("exe" -> "test"),
    "sickle" -> Map("exe" -> "test"),
    "cutadapt" -> Map("exe" -> "test"),
    "bwa" -> Map("exe" -> "test"),
    "star" -> Map("exe" -> "test"),
    "bowtie" -> Map("exe" -> "test"),
    "stampy" -> Map("exe" -> "test", "genome" -> "test", "hash" -> "test"),
    "samtools" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test")
  )
}