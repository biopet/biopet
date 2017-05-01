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
package nl.lumc.sasc.biopet.pipelines.gentrap

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.core.{BiopetFifoPipe, BiopetPipe}
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.extensions.gmap.Gsnap
import nl.lumc.sasc.biopet.extensions.hisat.Hisat2
import nl.lumc.sasc.biopet.extensions.tools.{BaseCounter, WipeReads}
import nl.lumc.sasc.biopet.utils.{ConfigUtils, Logging}
import nl.lumc.sasc.biopet.utils.config.Config
import nl.lumc.sasc.biopet.utils.camelize
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{AfterClass, DataProvider, Test}

abstract class GentrapTestAbstract(val expressionMeasures: List[String])
    extends TestNGSuite
    with Matchers {

  def initPipeline(map: Map[String, Any]): Gentrap = {
    new Gentrap() {
      override def configNamespace = "gentrap"
      override def globalConfig = new Config(map)
      // disable dict file check since it is based on the reference file name (which we can't modify here since
      // we use the mock /usr/bin/test file
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  def strandProtocols = Array("non_specific", "dutp")

  def aligner: Option[String] = None
  def removeRiboReads: Option[Boolean] = Some(false)
  def sample1: Boolean = true
  def sample2: Boolean = true
  def callVariants: Option[Boolean] = None

  @DataProvider(name = "expMeasuresstrandProtocol")
  def expMeasuresStrandProtocolProvider = {
    for {
      strandProtocol <- strandProtocols
    } yield Array(strandProtocol)
  }

  private var dirs: List[File] = Nil

  @Test(dataProvider = "expMeasuresstrandProtocol")
  def testGentrap(strandProtocol: String) = {
    val outputDir = GentrapTest.outputDir
    dirs :+= outputDir
    val settings = Map(
      "output_dir" -> outputDir,
      "gsnap" -> Map("db" -> "test", "dir" -> "test"),
      "expression_measures" -> expressionMeasures,
      "strand_protocol" -> strandProtocol
    ) ++
      aligner.map("aligner" -> _) ++
      removeRiboReads.map("remove_ribosomal_reads" -> _) ++
      callVariants.map("call_variants" -> _)
    val configs: List[Option[Map[String, Any]]] = List(
      Some(settings),
      (if (sample1) Some(GentrapTest.sample1) else None),
      (if (sample2) Some(GentrapTest.sample2) else None))
    val config =
      configs.flatten.foldLeft(GentrapTest.executables)((a, b) => ConfigUtils.mergeMaps(a, b))
    val gentrap: Gentrap = initPipeline(config)

    val numSamples = (sample1, sample2) match {
      case (true, true) => 2
      case (_, true) => 1
      case (true, _) => 1
      case _ => 0
    }

    if (numSamples == 0) {
      intercept[IllegalArgumentException] {
        gentrap.script()
      }
      Logging.errors.clear()
    } else {
      gentrap.script()

      val functions = gentrap.functions
        .flatMap {
          case f: BiopetFifoPipe => f.pipesJobs
          case f: BiopetPipe => f.pipesJobs
          case f => List(f)
        }
        .groupBy(_.getClass)

      gentrap.shivaVariantcalling.isDefined shouldBe callVariants.getOrElse(false)

      gentrap.summarySettings
        .getOrElse("expression_measures", List())
        .asInstanceOf[List[String]]
        .sorted shouldBe
        expressionMeasures.map(camelize(_)).sorted
      gentrap.summarySettings.get("call_variants") shouldBe Some(callVariants.getOrElse(false))
      gentrap.summarySettings.get("remove_ribosomal_reads") shouldBe Some(
        removeRiboReads.getOrElse(false))
      gentrap.summarySettings.get("strand_protocol") shouldBe Some(camelize(strandProtocol))

      if (expressionMeasures.contains("fragments_per_gene"))
        assert(gentrap.functions.exists(_.isInstanceOf[HtseqCount]))

      if (expressionMeasures.contains("fragments_per_exon"))
        assert(gentrap.functions.exists(_.isInstanceOf[HtseqCount]))

      if (expressionMeasures.contains("base_counts"))
        gentrap.functions.count(_.isInstanceOf[BaseCounter]) shouldBe numSamples

      if (expressionMeasures.contains("cufflinks_strict")) {
        assert(gentrap.functions.exists(_.isInstanceOf[Cufflinks]))
        assert(gentrap.functions.exists(_.isInstanceOf[Ln]))
      }

      if (expressionMeasures.contains("cufflinks_guided")) {
        assert(gentrap.functions.exists(_.isInstanceOf[Cufflinks]))
        assert(gentrap.functions.exists(_.isInstanceOf[Ln]))
      }

      if (expressionMeasures.contains("cufflinks_blind")) {
        assert(gentrap.functions.exists(_.isInstanceOf[Cufflinks]))
        assert(gentrap.functions.exists(_.isInstanceOf[Ln]))
      }

      gentrap.removeRibosomalReads shouldBe removeRiboReads.getOrElse(false)
      gentrap.functions.exists(_.isInstanceOf[WipeReads]) shouldBe removeRiboReads.getOrElse(false)

      val classMap = Map(
        "gsnap" -> classOf[Gsnap],
        "tophat" -> classOf[Tophat],
        "star" -> classOf[Star],
        "star-2pass" -> classOf[Star],
        "hisat2" -> classOf[Hisat2]
      )

      val alignerClass = classMap.get(aligner.getOrElse("gsnap"))

      alignerClass.foreach(c => assert(functions.keys.exists(_ == c)))
      classMap.values
        .filterNot(Some(_) == alignerClass)
        .foreach(x => assert(!functions.keys.exists(_ == x)))
    }
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    dirs.foreach(FileUtils.deleteDirectory)
  }
}

class GentrapFragmentsPerGeneTest extends GentrapTestAbstract(List("fragments_per_gene"))
//class GentrapFragmentsPerExonTest extends GentrapTestAbstract("fragments_per_exon")
class GentrapBaseCountsTest extends GentrapTestAbstract(List("base_counts"))
class GentrapCufflinksStrictTest extends GentrapTestAbstract(List("cufflinks_strict"))
class GentrapCufflinksGuidedTest extends GentrapTestAbstract(List("cufflinks_guided"))
class GentrapCufflinksBlindTest extends GentrapTestAbstract(List("cufflinks_blind"))
class GentrapAllTest
    extends GentrapTestAbstract(
      List("fragments_per_gene",
           "base_counts",
           "cufflinks_strict",
           "cufflinks_guided",
           "cufflinks_blind"))
class GentrapNoSamplesTest extends GentrapTestAbstract(List("fragments_per_gene")) {
  override def sample1 = false
  override def sample2 = false
}
class GentrapRemoveRibosomeTest extends GentrapTestAbstract(List("fragments_per_gene")) {
  override def removeRiboReads = Some(true)
}
class GentrapCallVariantsTest extends GentrapTestAbstract(List("fragments_per_gene")) {
  override def callVariants = Some(true)
}

object GentrapTest {
  def outputDir = Files.createTempDir()
  val inputDir = Files.createTempDir()

  def inputTouch(name: String): String = {
    val file = new File(inputDir, name)
    Files.touch(file)
    file.getAbsolutePath
  }

  private def copyFile(name: String): Unit = {
    val is = getClass.getResourceAsStream("/" + name)
    val os = new FileOutputStream(new File(inputDir, name))
    org.apache.commons.io.IOUtils.copy(is, os)
    os.close()
  }

  copyFile("ref.fa")
  copyFile("ref.dict")
  copyFile("ref.fa.fai")

  val executables: Map[String, Any] = Map(
    "skip_write_dependencies" -> true,
    "reference_fasta" -> (inputDir + File.separator + "ref.fa"),
    "refFlat" -> (inputDir + File.separator + "ref.fa"),
    "annotation_gtf" -> (inputDir + File.separator + "ref.fa"),
    "annotation_bed" -> (inputDir + File.separator + "ref.fa"),
    "annotation_refflat" -> (inputDir + File.separator + "ref.fa"),
    "ribosome_refflat" -> (inputDir + File.separator + "ref.fa"),
    "varscan_jar" -> "test",
    "rscript" -> Map("exe" -> "test"),
    "igvtools" -> Map("exe" -> "test", "igvtools_jar" -> "test"),
    "gatk_jar" -> "test"
  ) ++ Seq(
    // fastqc executables
    "fastqc",
    "seqtk",
    "sickle",
    "cutadapt",
    // mapping executables
    "star",
    "bowtie",
    "samtools",
    "gsnap",
    "tophat",
    // gentrap executables
    "cufflinks",
    "htseqcount",
    "grep",
    "pdflatex",
    "rscript",
    "tabix",
    "bgzip",
    "bedtoolscoverage",
    "md5sum",
    // bam2wig executables
    "wigtobigwig"
  ).map { case exe => exe -> Map("exe" -> "test") }.toMap

  val sample1: Map[String, Any] = Map(
    "samples" -> Map(
      "sample1" -> Map(
        "libraries" -> Map(
          "lib1" -> Map(
            "R1" -> inputTouch("1_1_R1.fq"),
            "R2" -> inputTouch("1_1_R2.fq")
          )
        ))))

  val sample2: Map[String, Any] = Map(
    "samples" -> Map(
      "sample3" -> Map(
        "libraries" -> Map(
          "lib1" -> Map(
            "R1" -> inputTouch("2_1_R1.fq"),
            "R2" -> inputTouch("2_1_R2.fq")
          ),
          "lib2" -> Map(
            "R1" -> inputTouch("2_2_R1.fq"),
            "R2" -> inputTouch("2_2_R2.fq")
          )
        ))))
}
