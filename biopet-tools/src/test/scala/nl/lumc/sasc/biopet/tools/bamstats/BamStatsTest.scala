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
package nl.lumc.sasc.biopet.tools.bamstats

import java.io.File
import java.nio.file.Paths

import com.google.common.io.Files
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvan_thof on 21-7-16.
  */
class BamStatsTest extends TestNGSuite with Matchers {
  @Test
  def testMain(): Unit = {
    val outputDir = Files.createTempDir()
    outputDir.deleteOnExit()
    BamStats.main(
      Array("-b", BamStatsTest.pairedBam01.getAbsolutePath, "-o", outputDir.getAbsolutePath))

    new File(outputDir, "bamstats.json") should exist
    new File(outputDir, "bamstats.summary.json") should exist

    new File(outputDir, "flagstats.tsv") shouldNot exist
    new File(outputDir, "insertsize.stats.tsv") shouldNot exist
    new File(outputDir, "insertsize.histogram.tsv") shouldNot exist
    new File(outputDir, "mappingQuality.stats.tsv") shouldNot exist
    new File(outputDir, "mappingQuality.histogram.tsv") shouldNot exist
    new File(outputDir, "clipping.stats.tsv") shouldNot exist
    new File(outputDir, "clipping.histogram.tsv") shouldNot exist

    new File(outputDir, "flagstats") shouldNot exist
    new File(outputDir, "flagstats.summary.json") shouldNot exist
    new File(outputDir, "mapping_quality.tsv") shouldNot exist
    new File(outputDir, "insert_size.tsv") shouldNot exist
    new File(outputDir, "clipping.tsv") shouldNot exist
    new File(outputDir, "left_clipping.tsv") shouldNot exist
    new File(outputDir, "right_clipping.tsv") shouldNot exist
    new File(outputDir, "5_prime_clipping.tsv") shouldNot exist
    new File(outputDir, "3_prime_clipping.tsv") shouldNot exist
  }

  @Test
  def testTsvOutputs(): Unit = {
    val outputDir = Files.createTempDir()
    outputDir.deleteOnExit()
    BamStats.main(
      Array("-b",
            BamStatsTest.pairedBam01.getAbsolutePath,
            "-o",
            outputDir.getAbsolutePath,
            "--tsvOutputs"))

    new File(outputDir, "bamstats.json") should exist
    new File(outputDir, "bamstats.summary.json") should exist

    new File(outputDir, "flagstats.tsv") should exist
    new File(outputDir, "insertsize.stats.tsv") should exist
    new File(outputDir, "insertsize.histogram.tsv") should exist
    new File(outputDir, "mappingQuality.stats.tsv") should exist
    new File(outputDir, "mappingQuality.histogram.tsv") should exist
    new File(outputDir, "clipping.stats.tsv") should exist
    new File(outputDir, "clipping.histogram.tsv") should exist

    new File(outputDir, "flagstats") shouldNot exist
    new File(outputDir, "flagstats.summary.json") shouldNot exist
    new File(outputDir, "mapping_quality.tsv") shouldNot exist
    new File(outputDir, "insert_size.tsv") shouldNot exist
    new File(outputDir, "clipping.tsv") shouldNot exist
    new File(outputDir, "left_clipping.tsv") shouldNot exist
    new File(outputDir, "right_clipping.tsv") shouldNot exist
    new File(outputDir, "5_prime_clipping.tsv") shouldNot exist
    new File(outputDir, "3_prime_clipping.tsv") shouldNot exist
  }
}

object BamStatsTest {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val pairedBam01 = new File(resourcePath("/paired01.bam"))
}
