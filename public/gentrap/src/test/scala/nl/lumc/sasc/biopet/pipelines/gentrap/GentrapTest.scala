/**
 * Copyright (c) 2015 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.pipelines.gentrap

import java.io.File

import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ AfterClass, DataProvider, Test }

import nl.lumc.sasc.biopet.core.config.Config
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.utils.ConfigUtils

class GentrapTest extends TestNGSuite with Matchers {

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

  /** Convenience method for making sample config */
  private def makeSampleConfig(sampleIdx: Int, numLibs: Int, paired: String) =
    Map("samples" ->
      Map(s"sample_$sampleIdx" ->
        Map("libraries" ->
          (0 to numLibs)
            // if paired == "mixed", alternate paired/not paired between each number
            .map(n => makeLibConfig(n, if (paired == "mixed") n % 2 == 0 else paired == "paired"))
            .toMap
        )
      )
    )

  private val pairedOneSampleOneLib = makeSampleConfig(1, 1, "paired")
  private val pairedOneSampleTwoLib = makeSampleConfig(1, 2, "paired")
  private val pairedOneSampleThreeLib = makeSampleConfig(1, 3, "paired")

  private lazy val validExpressionMeasures = Set(
    "fragments_per_gene", "fragments_per_exon", "bases_per_gene", "bases_per_exon",
    "cufflinks_strict", "cufflinks_guided", "cufflinks_blind")

  @DataProvider(name = "expMeasures_strandProtocol")
  def flexiprepOptions = {

    val strandProtocols = Array("non_specific", "dutp")
    // get all possible combinations of expression measures
    val expressionMeasures = validExpressionMeasures
      .subsets
      .map(_.toList)
      .toArray

    for {
      expressionMeasure <- expressionMeasures
      strandProtocol <- strandProtocols
    } yield Array(expressionMeasure, strandProtocol)
  }

  @Test(dataProvider = "expMeasures_strandProtocol")
  def testGentrap(expMeasures: List[String], strandProtocol: String) = {

    val settings = Map(
      "output_dir" -> GentrapTest.outputDir,
      "gsnap" -> Map("db" -> "test", "dir" -> "test"),
      "aligner" -> "gsnap",
      "expression_measures" -> expMeasures,
      "strand_protocol" -> strandProtocol
    )
    val config = ConfigUtils.mergeMaps(settings ++ pairedOneSampleOneLib, Map(GentrapTest.executables.toSeq: _*))
    val gentrap: Gentrap = initPipeline(config)

    gentrap.script()
    val functions = gentrap.functions.groupBy(_.getClass)

    functions(classOf[Gsnap]).size should be >= 1
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
      "star", "bowtie", "samtools", "gsnap",
      // gentrap executables
      "cufflinks", "htseq-count", "grep", "pdflatex", "Rscript", "tabix", "bgzip"
    ).map { case exe => exe -> Map("exe" -> "test") }.toMap
}
