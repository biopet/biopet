/**
 * Copyright (c) 2015 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wai Yi Leung <w.y.leung@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import htsjdk.samtools.fastq.{ FastqReader, FastqRecord }
import nl.lumc.sasc.biopet.core.config.Configurable
import org.mockito.Mockito.{ inOrder => inOrd, when }
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ DataProvider, Test }

import scala.collection.JavaConverters._

class SeqstatSolexaTest extends TestNGSuite with MockitoSugar with Matchers {

  import nl.lumc.sasc.biopet.tools.Seqstat._

  private def resourceFile(p: String): File =
    new File(resourcePath(p))

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource(p).toURI).toString

  // Helper functions to create iterator over FastqRecords given its IDs as Ints
  private def solexaRecordsOver(ids: String*): java.util.Iterator[FastqRecord] = ids
    .map(x => new FastqRecord(x, "ACTGNACTGN", "", "abcd@ABCD@"))
    .toIterator.asJava

  @DataProvider(name = "mockReaderProvider")
  def mockReaderProvider() =
    Array(
      Array(mock[FastqReader])
    )
  //
  //  @Test(dataProvider = "mockReaderProvider", groups = Array("solexa"))
  //  def testEncodingDetectionSolexa(refMock: FastqReader) = {
  //    when(refMock.iterator) thenReturn solexaRecordsOver("1", "2", "3", "4", "5")
  //
  //    val seqstat = Seqstat
  //    val numReads = seqstat.seqStat(refMock)
  //    numReads shouldBe 5
  //    seqstat.summarize()
  //
  //    seqstat.phred_correction shouldBe 64
  //    seqstat.phred_encoding shouldBe "solexa"
  //  }
  //
  //  @Test(dataProvider = "mockReaderProvider", groups = Array("solexa"))
  //  def testBaseCount(refMock: FastqReader) = {
  //    when(refMock.iterator) thenReturn solexaRecordsOver("1", "2", "3", "4", "5")
  //
  //    val seqstat = Seqstat
  //    val numReads = seqstat.seqStat(refMock)
  //    numReads shouldBe 5
  //
  //    seqstat.summarize()
  //
  //    seqstat.quals.sum shouldBe 50
  //    seqstat.readStats.withN shouldBe 5
  //    seqstat.nucleotideHistoMap('N') shouldBe 10
  //  }

  @Test def testArgsMinimum() = {
    val args = Array(
      "-i", resourcePath("/paired01a.fq"))
    val parsed = parseArgs(args)
    parsed.fastq shouldBe resourceFile("/paired01a.fq")
  }
}