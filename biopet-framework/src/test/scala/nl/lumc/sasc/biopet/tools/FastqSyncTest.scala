/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths
import scala.collection.JavaConverters._

import htsjdk.samtools.fastq.{ BasicFastqWriter, FastqReader, FastqRecord }
import org.mockito.Mockito.{ inOrder => inOrd, when }
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ DataProvider, Test }

class FastqSyncTest extends TestNGSuite with MockitoSugar with Matchers {

  import FastqSync._

  private def resourceFile(p: String): File =
    new File(resourcePath(p))

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource(p).toURI).toString

  // Helper functions to create iterator over FastqRecords given its IDs as Ints
  private def recordsOver(ids: String*): java.util.Iterator[FastqRecord] = ids
    .map(x => new FastqRecord(x, "A", "", "H"))
    .toIterator.asJava

  @DataProvider(name = "mockReaderProvider")
  def mockReaderProvider() =
    Array(
      Array(mock[FastqReader], mock[FastqReader], mock[FastqReader]))

  @Test(dataProvider = "mockReaderProvider")
  def testDefault(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")

    val sync = syncFastq(refMock, aMock, bMock)
    sync.result.length shouldBe 3
    sync.result(0) shouldBe (new FastqRecord("1", "A", "", "H"), new FastqRecord("1", "A", "", "H"))
    sync.result(1) shouldBe (new FastqRecord("2", "A", "", "H"), new FastqRecord("2", "A", "", "H"))
    sync.result(2) shouldBe (new FastqRecord("3", "A", "", "H"), new FastqRecord("3", "A", "", "H"))
    sync.numDiscard1 shouldBe 0
    sync.numDiscard2 shouldBe 0
    sync.numKept shouldBe 3
  }

  @Test(dataProvider = "mockReaderProvider")
  def testRefTooShort(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")

    val thrown = intercept[NoSuchElementException] {
      syncFastq(refMock, aMock, bMock)
    }
    thrown.getMessage should ===("Reference record stream shorter than expected")
  }

  @Test(dataProvider = "mockReaderProvider")
  def testSeqAEmpty(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver()
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")

    val sync = syncFastq(refMock, aMock, bMock)
    sync.result.length shouldBe 0
    sync.numDiscard1 shouldBe 0
    sync.numDiscard2 shouldBe 3
    sync.numKept shouldBe 0
  }

  @Test(dataProvider = "mockReaderProvider")
  def testSeqBEmpty(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver()

    val sync = syncFastq(refMock, aMock, bMock)
    sync.result.length shouldBe 0
    sync.numDiscard1 shouldBe 3
    sync.numDiscard2 shouldBe 0
    sync.numKept shouldBe 0
  }

  @Test(dataProvider = "mockReaderProvider")
  def testSeqAShorter(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")

    val sync = syncFastq(refMock, aMock, bMock)
    sync.result.length shouldBe 2
    sync.result(0) shouldBe (new FastqRecord("2", "A", "", "H"), new FastqRecord("2", "A", "", "H"))
    sync.result(1) shouldBe (new FastqRecord("3", "A", "", "H"), new FastqRecord("3", "A", "", "H"))
    sync.numDiscard1 shouldBe 0
    sync.numDiscard2 shouldBe 1
    sync.numKept shouldBe 2
  }

  @Test(dataProvider = "mockReaderProvider")
  def testSeqBShorter(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")

    val sync = syncFastq(refMock, aMock, bMock)
    sync.result.length shouldBe 2
    sync.result(0) shouldBe (new FastqRecord("2", "A", "", "H"), new FastqRecord("2", "A", "", "H"))
    sync.result(1) shouldBe (new FastqRecord("3", "A", "", "H"), new FastqRecord("3", "A", "", "H"))
    sync.numDiscard1 shouldBe 0
    sync.numDiscard2 shouldBe 1
    sync.numKept shouldBe 2
  }

  @Test(dataProvider = "mockReaderProvider")
  def testSeqABShorter(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2")

    val sync = syncFastq(refMock, aMock, bMock)
    sync.result.length shouldBe 1
    sync.result(0) shouldBe (new FastqRecord("2", "A", "", "H"), new FastqRecord("2", "A", "", "H"))
    sync.numDiscard1 shouldBe 1
    sync.numDiscard2 shouldBe 1
    sync.numKept shouldBe 1
  }

  @Test(dataProvider = "mockReaderProvider")
  def testSeqABShorterPairMarkSlash(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1/1", "2/1", "3/1")
    when(aMock.iterator) thenReturn recordsOver("2/1", "3/1")
    when(bMock.iterator) thenReturn recordsOver("1/2", "2/2")

    val sync = syncFastq(refMock, aMock, bMock)
    sync.result.length shouldBe 1
    sync.result(0) shouldBe (new FastqRecord("2/1", "A", "", "H"), new FastqRecord("2/2", "A", "", "H"))
    sync.numDiscard1 shouldBe 1
    sync.numDiscard2 shouldBe 1
    sync.numKept shouldBe 1
  }

  @Test(dataProvider = "mockReaderProvider")
  def testSeqABShorterPairMarkUnderscore(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1_1", "2_1", "3_1")
    when(aMock.iterator) thenReturn recordsOver("2_1", "3_1")
    when(bMock.iterator) thenReturn recordsOver("1_2", "2_2")

    val sync = syncFastq(refMock, aMock, bMock)
    sync.result.length shouldBe 1
    sync.result(0) shouldBe (new FastqRecord("2_1", "A", "", "H"), new FastqRecord("2_2", "A", "", "H"))
    sync.numDiscard1 shouldBe 1
    sync.numDiscard2 shouldBe 1
    sync.numKept shouldBe 1
  }

  @Test(dataProvider = "mockReaderProvider")
  def testSeqABShorterWithDesc(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1 desc1b", "2 desc2b", "3 desc3b")
    when(aMock.iterator) thenReturn recordsOver("2 desc2a", "3 desc3a")
    when(bMock.iterator) thenReturn recordsOver("1 desc1b", "2 desc2b")

    val sync = syncFastq(refMock, aMock, bMock)
    sync.result.length shouldBe 1
    sync.result(0) shouldBe (new FastqRecord("2 desc2a", "A", "", "H"), new FastqRecord("2 desc2b", "A", "", "H"))
    sync.numDiscard1 shouldBe 1
    sync.numDiscard2 shouldBe 1
    sync.numKept shouldBe 1
  }

  @Test(dataProvider = "mockReaderProvider")
  def testComplex(refMock: FastqReader, aMock: FastqReader, bMock: FastqReader) = {
    when(refMock.iterator) thenReturn recordsOver("1/2 yep", "2/2 yep", "3/2 yep", "4/2 yep", "5/2 yep")
    when(aMock.iterator) thenReturn recordsOver("1/1 yep", "2/1 yep", "4/1 yep")
    when(bMock.iterator) thenReturn recordsOver("1/2 yep", "3/2 yep", "4/2 yep")

    val sync = syncFastq(refMock, aMock, bMock)
    sync.result.length shouldBe 2
    sync.result(0) shouldBe (new FastqRecord("1/1 yep", "A", "", "H"), new FastqRecord("1/2 yep", "A", "", "H"))
    sync.result(1) shouldBe (new FastqRecord("4/1 yep", "A", "", "H"), new FastqRecord("4/2 yep", "A", "", "H"))
    sync.numDiscard1 shouldBe 1
    sync.numDiscard2 shouldBe 1
    sync.numKept shouldBe 2
  }

  @Test def testWriteSynced() = {
    val aMock = mock[BasicFastqWriter]
    val bMock = mock[BasicFastqWriter]
    val sync = SyncResult(Stream(
      (new FastqRecord("1", "A", "", "H"), new FastqRecord("1", "T", "", "E")),
      (new FastqRecord("2", "A", "", "H"), new FastqRecord("2", "T", "", "E"))),
      4, 3, 2)
    val obs = inOrd(aMock, bMock)
    val stdout = new java.io.ByteArrayOutputStream
    Console.withOut(stdout) {
      writeSyncedFastq(sync, aMock, bMock)
    }
    stdout.toString should ===(List(
      "Filtered 4 reads from first read file.",
      "Filtered 3 reads from second read file.",
      "Synced read files contain 2 reads.\n"
    ).mkString("\n"))
    obs.verify(aMock).write(new FastqRecord("1", "A", "", "H"))
    obs.verify(bMock).write(new FastqRecord("1", "T", "", "E"))
    obs.verify(aMock).write(new FastqRecord("2", "A", "", "H"))
    obs.verify(bMock).write(new FastqRecord("2", "T", "", "E"))
  }

  @Test def testArgsMinimum() = {
    val args = Array(
      "-r", resourcePath("/paired01a.fq"),
      "-i", resourcePath("/paired01a.fq"),
      "-j", resourcePath("/paired01b.fq"),
      "-o", "/tmp/mockout1.fq",
      "-p", "/tmp/mockout2.fq")
    val parsed = parseArgs(args)
    parsed.refFastq shouldBe resourceFile("/paired01a.fq")
    parsed.inputFastq1 shouldBe resourceFile("/paired01a.fq")
    parsed.inputFastq2 shouldBe resourceFile("/paired01b.fq")
    parsed.outputFastq1 shouldBe new File("/tmp/mockout1.fq")
    parsed.outputFastq2 shouldBe new File("/tmp/mockout2.fq")
  }
}