package nl.lumc.sasc.biopet.tools.refflatstats

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
  * Created by pjvan_thof on 23-5-17.
  */
class RefflatStatsTest extends TestNGSuite with Matchers {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def test: Unit = {
    val geneOutput = File.createTempFile("gene.", ".tsv")
    geneOutput.deleteOnExit()
    val transcriptOutput = File.createTempFile("transcript.", ".tsv")
    transcriptOutput.deleteOnExit()
    val exonOutput = File.createTempFile("exon.", ".tsv")
    exonOutput.deleteOnExit()
    val intronOutput = File.createTempFile("intron.", ".tsv")
    intronOutput.deleteOnExit()
    val refflatFile = new File(resourcePath("/chrQ.refflat"))
    val fastaFile = new File(resourcePath("/fake_chrQ.fa"))
    RefflatStats.main(
      Array(
        "--geneOutput",
        geneOutput.getAbsolutePath,
        "--transcriptOutput",
        transcriptOutput.getAbsolutePath,
        "--exonOutput",
        exonOutput.getAbsolutePath,
        "--intronOutput",
        intronOutput.getAbsolutePath,
        "--annotationRefflat",
        refflatFile.getAbsolutePath,
        "--referenceFasta",
        fastaFile.getAbsolutePath
      ))

    val lines = Source.fromFile(geneOutput).getLines().toList

    lines(0) shouldBe "gene\tcontig\tstart\tend\ttotalGC\texonGc\tintronGc\tlength\texonLength"
    lines(1) shouldBe "geneA\tchrQ\t201\t500\t0.49\t0.5\t0.43999999999999995\t300\t197"

  }
}
