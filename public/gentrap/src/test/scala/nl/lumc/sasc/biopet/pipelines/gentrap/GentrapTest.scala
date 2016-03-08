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
package nl.lumc.sasc.biopet.pipelines.gentrap

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.extensions.tools.BaseCounter
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{DataProvider, Test}

abstract class GentrapTestAbstract(val expressionMeasure: String) extends TestNGSuite with Matchers {

  def initPipeline(map: Map[String, Any]): Gentrap = {
    new Gentrap() {
      override def configName = "gentrap"
      override def globalConfig = new Config(map)
      // disable dict file check since it is based on the reference file name (which we can't modify here since
      // we use the mock /usr/bin/test file
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  /** Convenience method for making library config */
  private def makeLibConfig(idx: Int, paired: Boolean = true) = {
    val files = Map("R1" -> GentrapTest.inputTouch("test_R1.fq"))
    if (paired) (s"lib_$idx", files ++ Map("R2" -> GentrapTest.inputTouch("test_R2.fq")))
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

  val validExpressionMeasures = Set(
    "fragments_per_gene", "fragments_per_exon", "base_counts",
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
      //expressionMeasure <- expressionMeasures
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

    if (expMeasures.contains("fragments_per_gene"))
      assert(gentrap.functions.exists(_.isInstanceOf[HtseqCount]))

    if (expMeasures.contains("fragments_per_exon"))
      assert(gentrap.functions.exists(_.isInstanceOf[HtseqCount]))

    if (expMeasures.contains("base_counts"))
      gentrap.functions.count(_.isInstanceOf[BaseCounter]) shouldBe numSamples

    if (expMeasures.contains("cufflinks_strict")) {
      assert(gentrap.functions.exists(_.isInstanceOf[Cufflinks]))
      assert(gentrap.functions.exists(_.isInstanceOf[Ln]))
    }

    if (expMeasures.contains("cufflinks_guided")) {
      assert(gentrap.functions.exists(_.isInstanceOf[Cufflinks]))
      assert(gentrap.functions.exists(_.isInstanceOf[Ln]))
    }

    if (expMeasures.contains("cufflinks_blind")) {
      assert(gentrap.functions.exists(_.isInstanceOf[Cufflinks]))
      assert(gentrap.functions.exists(_.isInstanceOf[Ln]))
    }
  }

}

class GentrapFragmentsPerGeneTest extends GentrapTestAbstract("fragments_per_gene")
//class GentrapFragmentsPerExonTest extends GentrapTestAbstract("fragments_per_exon")
class GentrapBaseCountsTest extends GentrapTestAbstract("base_counts")
class GentrapCufflinksStrictTest extends GentrapTestAbstract("cufflinks_strict")
class GentrapCufflinksGuidedTest extends GentrapTestAbstract("cufflinks_guided")
class GentrapCufflinksBlindTest extends GentrapTestAbstract("cufflinks_blind")

object GentrapTest {
  val outputDir = Files.createTempDir()
  outputDir.deleteOnExit()
  new File(outputDir, "input").mkdirs()
  def inputTouch(name: String): String = {
    val file = new File(outputDir, "input" + File.separator + name)
    Files.touch(file)
    file.getAbsolutePath
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

  val executables = Map(
    "reference_fasta" -> (outputDir + File.separator + "ref.fa"),
    "refFlat" -> (outputDir + File.separator + "ref.fa"),
    "annotation_gtf" -> (outputDir + File.separator + "ref.fa"),
    "annotation_bed" -> (outputDir + File.separator + "ref.fa"),
    "annotation_refflat" -> (outputDir + File.separator + "ref.fa"),
    "varscan_jar" -> "test",
    "rscript" -> Map("exe" -> "test")
  ) ++ Seq(
      // fastqc executables
      "fastqc", "seqtk", "sickle", "cutadapt",
      // mapping executables
      "star", "bowtie", "samtools", "gsnap", "tophat",
      // gentrap executables
      "cufflinks", "htseqcount", "grep", "pdflatex", "rscript", "tabix", "bgzip", "bedtoolscoverage", "md5sum",
      // bam2wig executables
      "igvtools", "wigtobigwig"
    ).map { case exe => exe -> Map("exe" -> "test") }.toMap
}
