/**
 * Copyright (c) 2015 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.pipelines.gentrap

import java.io.File

import com.google.common.io.Files
import nl.lumc.sasc.biopet.pipelines.gentrap.scripts.AggrBaseCount
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ AfterClass, DataProvider, Test }

import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.utils.ConfigUtils

class GentrapTest extends TestNGSuite with Matchers {

  import Gentrap._
  import Gentrap.ExpMeasures._
  import Gentrap.StrandProtocol._

  def initPipeline(map: Map[String, Any]): Gentrap = {
    new Gentrap() {
      override def configName = "gentrap"

      override def globalConfig = new Config(map)

      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  /** Convenience method for making library config */
  private def makeLibConfig(idx: Int, paired: Boolean = true) = {
    val files = Map("R1" -> "test_R1.fq")
    if (paired) (s"lib_$idx", files ++ Map("R2" -> "test_R2.fq"))
    else (s"lib_$idx", files)
  }

  /** Convenience type for sample config */
  private type SamplesConfig = Map[String, Map[String, Map[String, Map[String, Map[String, String]]]]]

  /** Convenience method for making a single sample config */
  private def makeSampleConfig(sampleIdx: Int, numLibs: Int, paired: Boolean) =
    (s"sample_$sampleIdx",
      Map("libraries" ->
        (1 to numLibs)
        .map(n => makeLibConfig(n, paired))
        .toMap
      )
    )

  /** Convenience method for making all samples config */
  private def makeSamplesConfig(numSamples: Int, numLibsEachSample: Int, pairMode: String): SamplesConfig =
    Map("samples" ->
      (1 to numSamples)
      // if paired == "mixed", alternate paired/not paired between each number
      .map(n => makeSampleConfig(n, numLibsEachSample, if (pairMode == "mixed") n % 2 == 0 else pairMode == "paired"))
      .toMap
    )

  private lazy val validExpressionMeasures = Set(
    "fragments_per_gene", "fragments_per_exon", "bases_per_gene", "bases_per_exon",
    "cufflinks_strict", "cufflinks_guided", "cufflinks_blind")

  @DataProvider(name = "expMeasuresstrandProtocol")
  def expMeasuresStrandProtocolProvider = {

    //val sampleConfigs = Array(pairedOneSampleOneLib, pairedOneSampleTwoLib, pairedOneSampleThreeLib)
    val sampleConfigs = for {
      (sampleNum, libNum) <- Seq(
        // check multiple libs for single run only ~ to trim down less-informative tests
        // need to check 2 and 3 samples since multi-sample plotting differs when sample is 1 or 2 and 3
        (1, 1), (1, 2), (2, 1), (3, 1)
      )
      libType <- Seq("paired", "single", "mixed")
    } yield makeSamplesConfig(sampleNum, libNum, libType)

    val strandProtocols = Array("non_specific", "dutp")
    // get all possible combinations of expression measures
    val expressionMeasures = validExpressionMeasures
      //.subsets
      //.map(_.toList)
      .toArray

    for {
      sampleConfig <- sampleConfigs.toArray
      expressionMeasure <- expressionMeasures
      strandProtocol <- strandProtocols
    } yield Array(sampleConfig, List(expressionMeasure), strandProtocol)
  }

  @Test(dataProvider = "expMeasuresstrandProtocol")
  def testGentrap(sampleConfig: SamplesConfig, expMeasures: List[String], strandProtocol: String) = {

    val settings = Map(
      "output_dir" -> GentrapTest.outputDir,
      "gsnap" -> Map("db" -> "test", "dir" -> "test"),
      "aligner" -> "gsnap",
      "expression_measures" -> expMeasures,
      "strand_protocol" -> strandProtocol
    )
    val config = ConfigUtils.mergeMaps(settings ++ sampleConfig, Map(GentrapTest.executables.toSeq: _*))
    val gentrap: Gentrap = initPipeline(config)

    gentrap.script()
    val functions = gentrap.functions.groupBy(_.getClass)
    val numSamples = sampleConfig("samples").size

    functions(classOf[Gsnap]).size should be >= 1

    if (expMeasures.contains("fragments_per_gene")) {
      gentrap.functions
        .collect { case x: HtseqCount => x.output.toString.endsWith(".fragments_per_gene") }.size shouldBe numSamples
    }

    if (expMeasures.contains("fragments_per_exon")) {
      gentrap.functions
        .collect { case x: HtseqCount => x.output.toString.endsWith(".fragments_per_exon") }.size shouldBe numSamples
    }

    if (expMeasures.contains("bases_per_gene")) {
      gentrap.functions
        .collect { case x: AggrBaseCount => x.output.toString.endsWith(".bases_per_gene") }.size shouldBe numSamples
    }

    if (expMeasures.contains("bases_per_exon")) {
      gentrap.functions
        .collect { case x: AggrBaseCount => x.output.toString.endsWith(".bases_per_exon") }.size shouldBe numSamples
    }

    if (expMeasures.contains("cufflinks_strict")) {
      gentrap.functions
        .collect {
          case x: Cufflinks => x.outputGenesFpkm.getParentFile.toString.endsWith("cufflinks_strict")
          case x: Ln => x.output.toString.endsWith(".genes_fpkm_cufflinks_strict") ||
            x.output.toString.endsWith(".isoforms_fpkm_cufflinks_strict")
        }
        .count(identity) shouldBe numSamples * 3 // three types of jobs per sample
    }

    if (expMeasures.contains("cufflinks_guided")) {
      gentrap.functions
        .collect {
          case x: Cufflinks => x.outputGenesFpkm.getParentFile.toString.endsWith("cufflinks_guided")
          case x: Ln => x.output.toString.endsWith(".genes_fpkm_cufflinks_guided") ||
            x.output.toString.endsWith(".isoforms_fpkm_cufflinks_guided")
        }
        .count(identity) shouldBe numSamples * 3 // three types of jobs per sample
    }

    if (expMeasures.contains("cufflinks_blind")) {
      gentrap.functions
        .collect {
          case x: Cufflinks => x.outputGenesFpkm.getParentFile.toString.endsWith("cufflinks_blind")
          case x: Ln => x.output.toString.endsWith(".genes_fpkm_cufflinks_blind") ||
            x.output.toString.endsWith(".isoforms_fpkm_cufflinks_blind")
        }
        .count(identity) shouldBe numSamples * 3 // three types of jobs per sample
    }
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    FileUtils.deleteDirectory(GentrapTest.outputDir)
  }
}

object GentrapTest {
  val outputDir = Files.createTempDir()

  val executables = Map(
    "reference" -> "test",
    "annotation_gtf" -> "test",
    "annotation_bed" -> "test",
    "annotation_refflat" -> "test",
    "varscan_jar" -> "test"
  ) ++ Seq(
      // fastqc executables
      "fastqc", "seqtk", "sickle", "cutadapt",
      // mapping executables
      "star", "bowtie", "samtools", "gsnap", "tophat",
      // gentrap executables
      "cufflinks", "htseqcount", "grep", "pdflatex", "Rscript", "tabix", "bgzip",
      // bam2wig executables
      "igvtools", "wigtobigwig"
    ).map { case exe => exe -> Map("exe" -> "test") }.toMap
}
