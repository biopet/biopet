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

  @Test(dataProvider = "mockReaderProvider", groups = Array("sanger"), singleThreaded = true)
  def testSeqCountReads(fqMock: FastqReader) = {
    when(fqMock.iterator) thenReturn recordsOver("1", "2", "3", "4", "5")

    val seqstat = Seqstat
    val numReads = seqstat.seqStat(fqMock)
    numReads shouldBe 5
  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("sanger"), singleThreaded = true)
  def testEncodingDetectionSanger(fqMock: FastqReader) = {
    when(fqMock.iterator) thenReturn recordsOver("1")

    val seqstat = Seqstat
    val numReads = seqstat.seqStat(fqMock)
    numReads shouldBe 1
    seqstat.summarize()

    seqstat.phred_correction shouldBe 33
    seqstat.phred_encoding shouldBe "sanger"
    //    nucleotideHistoMap.values.sum shouldBe 5
    //    nucleotideHistoMap('N') shouldBe 1
    //    nucleotideHistoMap('A') shouldBe 1
  }

  @Test def testArgsMinimum() = {
    val args = Array(
      "-i", resourcePath("/paired01a.fq"))
    val parsed = parseArgs(args)
    parsed.fastq shouldBe resourceFile("/paired01a.fq")
  }
}