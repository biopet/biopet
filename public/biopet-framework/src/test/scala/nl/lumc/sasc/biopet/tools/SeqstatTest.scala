/**
 * Copyright (c) 2015 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wai Yi Leung <w.y.leung@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import htsjdk.samtools.fastq.{ FastqReader, FastqRecord }
import org.mockito.Mockito.{ inOrder => inOrd, when }
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ DataProvider, Test }

import scala.collection.JavaConverters._

class SeqstatTest extends TestNGSuite with MockitoSugar with Matchers {

  import nl.lumc.sasc.biopet.tools.Seqstat._

  private def resourceFile(p: String): File =
    new File(resourcePath(p))

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource(p).toURI).toString

  // Helper functions to create iterator over FastqRecords given its IDs as Ints
  // Record with 'A' and Qual==39 (SangerEncoding)
  private def recordsOver(ids: String*): java.util.Iterator[FastqRecord] = ids
    .map(x => new FastqRecord(x, "ACGTN", "", "HIBC!"))
    .toIterator.asJava

  @DataProvider(name = "mockReaderProvider")
  def mockReaderProvider() =
    Array(
      Array(mock[FastqReader])
    )

  @Test(dataProvider = "mockReaderProvider", groups = Array("sanger"), singleThreaded = true)
  def testDefault(fqMock: FastqReader) = {
    when(fqMock.iterator) thenReturn recordsOver("1", "2", "3")
  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("read"), singleThreaded = true)
  def testSeqCountReads(fqMock: FastqReader) = {
    when(fqMock.iterator) thenReturn recordsOver("1", "2", "3", "4", "5")

    val seqstat = Seqstat
    val numReads = seqstat.seqStat(fqMock)
    numReads shouldBe 5
  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("phredscore"), singleThreaded = true, dependsOnGroups = Array("read"))
  def testEncodingDetectionSanger(fqMock: FastqReader) = {

    val seqstat = Seqstat
    //    val numReads = seqstat.seqStat(fqMock)
    //    numReads shouldBe 1
    seqstat.summarize()

    // TODO: divide into more testcases, instead of having in 1,
    // currently not possible because the Seqstat is a bit state dependent?
    seqstat.phred_correction shouldBe 33
    seqstat.phred_encoding shouldBe "sanger"
  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("nucleocount"), singleThreaded = true, dependsOnGroups = Array("phredscore"))
  def testEncodingNucleotideCount(fqMock: FastqReader) = {

    val seqstat = Seqstat
    baseHistogram(40) shouldEqual 5
    baseHistogram(39) shouldEqual 10
    baseHistogram(34) shouldEqual 15
    baseHistogram(33) shouldEqual 20
    baseHistogram(0) shouldEqual 20
  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("basehistogram"), singleThreaded = true, dependsOnGroups = Array("nucleocount"))
  def testEncodingBaseHistogram(fqMock: FastqReader) = {

    val seqstat = Seqstat
    baseHistogram(40) shouldEqual 5
    baseHistogram(39) shouldEqual 10
    baseHistogram(34) shouldEqual 15
    baseHistogram(33) shouldEqual 20
    baseHistogram(0) shouldEqual 20
  }

  @Test def testArgsMinimum() = {
    val args = Array(
      "-i", resourcePath("/paired01a.fq"))
    val parsed = parseArgs(args)
    parsed.fastq shouldBe resourceFile("/paired01a.fq")
  }
}