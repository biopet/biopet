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
  def testMain: Unit = {
    val outputDir = Files.createTempDir()
    outputDir.deleteOnExit()
    BamStats.main(Array("-b", BamStatsTest.pairedBam01.getAbsolutePath, "-o", outputDir.getAbsolutePath))

    new File(outputDir, "flagstats") should exist
    new File(outputDir, "flagstats.summary.json") should exist
    new File(outputDir, "mapping_quality.tsv") should exist
    new File(outputDir, "insert_size.tsv") should exist
    new File(outputDir, "clipping.tsv") should exist
    new File(outputDir, "left_clipping.tsv") should exist
    new File(outputDir, "right_clipping.tsv") should exist
    new File(outputDir, "5_prime_clipping.tsv") should exist
    new File(outputDir, "3_prime_clipping.tsv") should exist
  }
}

object BamStatsTest {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val pairedBam01 = new File(resourcePath("/paired01.bam"))
}