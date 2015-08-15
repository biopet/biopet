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

import java.io.{ File, FileOutputStream }

import com.google.common.io.Files
import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.extensions.bowtie.Bowtie
import nl.lumc.sasc.biopet.extensions.bwa.{ BwaAln, BwaMem, BwaSampe, BwaSamse }
import nl.lumc.sasc.biopet.extensions.picard.{ AddOrReplaceReadGroups, MarkDuplicates, MergeSamFiles, SortSam }
import nl.lumc.sasc.biopet.pipelines.flexiprep.{ Cutadapt, Fastqc, SeqtkSeq }
import nl.lumc.sasc.biopet.tools.{ FastqSync, SeqStat }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ AfterClass, DataProvider, Test }

/**
 * Test class for [[Mapping]]
 *
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

  @DataProvider(name = "mappingOptions")
  def mappingOptions = {
    val aligners = Array("bwa-mem", "bwa-aln", "star", "star-2pass", "bowtie", "stampy", "gsnap", "tophat")
    val paired = Array(true, false)
    val chunks = Array(1, 5, 10, 100)
    val skipMarkDuplicates = Array(true, false)
    val skipFlexipreps = Array(true, false)
    val zipped = Array(true, false)

    for (
      aligner <- aligners;
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
      mapping.input_R1 = new File(mapping.outputDir, "bla_R1.fq.gz")
      if (paired) mapping.input_R2 = Some(new File(mapping.outputDir, "bla_R2.fq.gz"))
    } else {
      mapping.input_R1 = new File(mapping.outputDir, "bla_R1.fq")
      if (paired) mapping.input_R2 = Some(new File(mapping.outputDir, "bla_R2.fq"))
    }
    mapping.sampleId = Some("1")
    mapping.libId = Some("1")
    mapping.script()

    //Flexiprep
    mapping.functions.count(_.isInstanceOf[Fastqc]) shouldBe (if (skipFlexiprep) 0 else if (paired) 4 else 2)
    mapping.functions.count(_.isInstanceOf[Zcat]) shouldBe (if (!zipped || (chunks > 1 && skipFlexiprep)) 0 else if (paired) 2 else 1)
    mapping.functions.count(_.isInstanceOf[SeqStat]) shouldBe ((if (skipFlexiprep) 0 else if (paired) 4 else 2) * chunks)
    mapping.functions.count(_.isInstanceOf[SeqtkSeq]) shouldBe ((if (skipFlexiprep) 0 else if (paired) 2 else 1) * chunks)
    mapping.functions.count(_.isInstanceOf[Cutadapt]) shouldBe ((if (skipFlexiprep) 0 else if (paired) 2 else 1) * chunks)
    mapping.functions.count(_.isInstanceOf[FastqSync]) shouldBe ((if (skipFlexiprep) 0 else if (paired && !skipFlexiprep) 1 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[Sickle]) shouldBe ((if (skipFlexiprep) 0 else 1) * chunks)
    mapping.functions.count(_.isInstanceOf[Gzip]) shouldBe (if (skipFlexiprep) 0 else if (paired) 2 else 1)

    //aligners
    mapping.functions.count(_.isInstanceOf[BwaMem]) shouldBe ((if (aligner == "bwa-mem") 1 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[BwaAln]) shouldBe ((if (aligner == "bwa-aln") if (paired) 2 else 1 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[BwaSampe]) shouldBe ((if (aligner == "bwa-aln") if (paired) 1 else 0 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[BwaSamse]) shouldBe ((if (aligner == "bwa-aln") if (paired) 0 else 1 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[Star]) shouldBe ((if (aligner == "star") 1 else if (aligner == "star-2pass") 3 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[Bowtie]) shouldBe ((if (aligner == "bowtie") 1 else 0) * chunks)
    mapping.functions.count(_.isInstanceOf[Stampy]) shouldBe ((if (aligner == "stampy") 1 else 0) * chunks)

    // Sort sam or replace readgroup
    val sort = aligner match {
      case "bwa-mem" | "bwa-aln" | "stampy" => "sortsam"
      case "star" | "star-2pass" | "bowtie" | "gsnap" | "tophat" => "replacereadgroups"
      case _ => throw new IllegalArgumentException("aligner: " + aligner + " does not exist")
    }

    if (aligner != "tophat") { // FIXME
      mapping.functions.count(_.isInstanceOf[SortSam]) shouldBe ((if (sort == "sortsam") 1 else 0) * chunks)
      mapping.functions.count(_.isInstanceOf[AddOrReplaceReadGroups]) shouldBe ((if (sort == "replacereadgroups") 1 else 0) * chunks)
      mapping.functions.count(_.isInstanceOf[MergeSamFiles]) shouldBe (if (skipMarkDuplicate && chunks > 1) 1 else 0)
      mapping.functions.count(_.isInstanceOf[MarkDuplicates]) shouldBe (if (skipMarkDuplicate) 0 else 1)
    }
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    FileUtils.deleteDirectory(MappingTest.outputDir)
  }
}

object MappingTest {
  val outputDir = Files.createTempDir()

  private def copyFile(name: String): Unit = {
    val is = getClass.getResourceAsStream("/" + name)
    val os = new FileOutputStream(new File(outputDir, name))
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  copyFile("ref.fa")
  copyFile("ref.dict")
  copyFile("ref.fa.fai")

  val executables = Map(
    "reference_fasta" -> (outputDir + File.separator + "ref.fa"),
    "db" -> "test",
    "bowtie_index" -> "test",
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