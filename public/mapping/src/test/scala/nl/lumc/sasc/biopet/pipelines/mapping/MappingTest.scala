package nl.lumc.sasc.biopet.pipelines.mapping

import java.io.File

import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ AfterClass, DataProvider, Test }

import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.extensions.bwa.{ BwaSamse, BwaSampe, BwaAln, BwaMem }
import nl.lumc.sasc.biopet.extensions.picard.{ MergeSamFiles, AddOrReplaceReadGroups, MarkDuplicates, SortSam }
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.pipelines.flexiprep.Cutadapt
import nl.lumc.sasc.biopet.pipelines.flexiprep.Fastqc
import nl.lumc.sasc.biopet.pipelines.flexiprep._
import nl.lumc.sasc.biopet.tools.FastqSync
import nl.lumc.sasc.biopet.utils.ConfigUtils

/**
 * Created by pjvan_thof on 2/12/15.
 */
class MappingTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): Mapping = {
    new Mapping {
      override def configName = "mapping"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "mappingOptions", parallel = true)
  def mappingOptions = {
    val aligners = Array("bwa", "bwa-aln", "star", "star-2pass", "bowtie", "stampy")
    val paired = Array(true, false)
    val chunks = Array(1, 5, 10, 100)
    val skipMarkDuplicates = Array(true, false)
    val skipFlexipreps = Array(true, false)

    for (
      aligner <- aligners;
      pair <- paired;
      chunk <- chunks;
      skipMarkDuplicate <- skipMarkDuplicates;
      skipFlexiprep <- skipFlexipreps
    ) yield Array(aligner, pair, chunk, skipMarkDuplicate, skipFlexiprep)
  }

  @Test(dataProvider = "mappingOptions")
  def testMapping(aligner: String, paired: Boolean, chunks: Int, skipMarkDuplicate: Boolean, skipFlexiprep: Boolean) = {
    val map = ConfigUtils.mergeMaps(Map("output_dir" -> MappingTest.outputDir,
      "aligner" -> aligner,
      "number_chunks" -> chunks,
      "skip_markduplicates" -> skipMarkDuplicate,
      "skip_flexiprep" -> skipFlexiprep
    ), Map(MappingTest.executables.toSeq: _*))
    val mapping: Mapping = initPipeline(map)

    mapping.input_R1 = new File(mapping.outputDir, "bla_R1.fq")
    if (paired) mapping.input_R2 = Some(new File(mapping.outputDir, "bla_R2.fq"))
    mapping.sampleId = Some("1")
    mapping.libId = Some("1")
    mapping.script()

    //Flexiprep
    mapping.functions.count(_.isInstanceOf[Fastqc]) shouldBe (if (skipFlexiprep) 0 else if (paired) 4 else 2)
    mapping.functions.count(_.isInstanceOf[Zcat]) shouldBe 0
    mapping.functions.count(_.isInstanceOf[SeqtkSeq]) shouldBe ((if (skipFlexiprep) 0 else if (paired) 2 else 1) * chunks)
    mapping.functions.count(_.isInstanceOf[Cutadapt]) shouldBe ((if (skipFlexiprep) 0 else if (paired) 2 else 1) * chunks)
    mapping.functions.count(_.isInstanceOf[FastqSync]) shouldBe ((if (skipFlexiprep) 0 else if (paired && !skipFlexiprep) 1 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[Sickle]) shouldBe ((if (skipFlexiprep) 0 else 1) * chunks)
    mapping.functions.count(_.isInstanceOf[Gzip]) shouldBe (if (skipFlexiprep) 0 else if (paired) 2 else 1)

    //aligners
    mapping.functions.count(_.isInstanceOf[BwaMem]) shouldBe ((if (aligner == "bwa") 1 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[BwaAln]) shouldBe ((if (aligner == "bwa-aln") (if (paired) 2 else 1) else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[BwaSampe]) shouldBe ((if (aligner == "bwa-aln") (if (paired) 1 else 0) else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[BwaSamse]) shouldBe ((if (aligner == "bwa-aln") (if (paired) 0 else 1) else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[Star]) shouldBe ((if (aligner == "star") 1 else if (aligner == "star-2pass") 3 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[Bowtie]) shouldBe ((if (aligner == "bowtie") 1 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[Stampy]) shouldBe ((if (aligner == "stampy") 1 else 0) * chunks)

    // Sort sam or replace readgroup
    val sort = aligner match {
      case "bwa" | "bwa-aln" | "stampy"     => "sortsam"
      case "star" | "star-2pass" | "bowtie" => "replacereadgroups"
      case _                                => throw new IllegalArgumentException("aligner: " + aligner + " does not exist")
    }

    mapping.functions.count(_.isInstanceOf[SortSam]) shouldBe ((if (sort == "sortsam") 1 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[AddOrReplaceReadGroups]) shouldBe ((if (sort == "replacereadgroups") 1 else 0) * chunks)

    mapping.functions.count(_.isInstanceOf[MergeSamFiles]) shouldBe (if (skipMarkDuplicate && chunks > 1) 1 else 0)
    mapping.functions.count(_.isInstanceOf[MarkDuplicates]) shouldBe (if (skipMarkDuplicate) 0 else 1)
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    FileUtils.deleteDirectory(MappingTest.outputDir)
  }
}

object MappingTest {
  val outputDir = Files.createTempDir()

  val executables = Map(
    "reference" -> "test",
    "seqstat" -> Map("exe" -> "test"),
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "sickle" -> Map("exe" -> "test"),
    "cutadapt" -> Map("exe" -> "test"),
    "bwa" -> Map("exe" -> "test"),
    "star" -> Map("exe" -> "test"),
    "bowtie" -> Map("exe" -> "test"),
    "stampy" -> Map("exe" -> "test", "genome" -> "test", "hash" -> "test"),
    "samtools" -> Map("exe" -> "test")
  )
}