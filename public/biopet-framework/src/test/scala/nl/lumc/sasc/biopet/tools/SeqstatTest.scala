/**
 * Copyright (c) 2015 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wai Yi Leung <w.y.leung@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import htsjdk.samtools.fastq.{ AsyncFastqWriter, FastqReader, FastqRecord }
import nl.lumc.sasc.biopet.tools.FastqSync._
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
    .map(x => new FastqRecord(x, "A", "", "H"))
    .toIterator.asJava

  private def solexaRecordsOver(ids: String*): java.util.Iterator[FastqRecord] = ids
    .map(x => new FastqRecord(x, "ACTGTNCGATAG", "", "abcde;ABCDEF"))
    .toIterator.asJava

  @DataProvider(name = "mockReaderProvider")
  def mockReaderProvider() =
    Array(
      Array(mock[FastqReader])
    )

  @Test(dataProvider = "mockReaderProvider")
  def testDefault(refMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
  }

  @Test(dataProvider = "mockReaderProvider")
  def testSeqCountReads(refMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2", "3", "4", "5")

    val (numReads) = seqStat(refMock)
    numReads shouldBe 5
  }

  @Test(dataProvider = "mockReaderProvider")
  def testSeqQuality(refMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2", "3", "4", "5")

    val (numReads) = seqStat(refMock)
    numReads shouldBe 5
  }

  @Test(dataProvider = "mockReaderProvider")
  def testEncodingDetectionSanger(refMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2", "3", "4", "5")

    val (numReads) = seqStat(refMock)
    numReads shouldBe 5

    summarize()

    phred_correction shouldBe 33
    phred_encoding shouldBe "sanger"
  }

  @Test(dataProvider = "mockReaderProvider")
  def testEncodingDetectionSolexa(refMock: FastqReader) = {
    when(refMock.iterator) thenReturn solexaRecordsOver("1", "2", "3", "4", "5")

    val (numReads) = seqStat(refMock)
    numReads shouldBe 5

    summarize()

    phred_correction shouldBe 64
    phred_encoding shouldBe "solexa"
  }

  @Test def testArgsMinimum() = {
    val args = Array(
      "-i", resourcePath("/paired01a.fq"))
    val parsed = parseArgs(args)
    parsed.fastq shouldBe resourceFile("/paired01a.fq")
  }
}