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

class SeqStatTest extends TestNGSuite with MockitoSugar with Matchers {

  import nl.lumc.sasc.biopet.tools.FqEncoding._
  import nl.lumc.sasc.biopet.tools.SeqStat._

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

    val seqstat = SeqStat
    val numReads = seqstat.seqStat(fqMock)
    numReads shouldBe 5
  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("phredscore"), singleThreaded = true, dependsOnGroups = Array("read"))
  def testEncodingDetectionSanger(fqMock: FastqReader) = {

    val seqstat = SeqStat
    seqstat.summarize()

    seqstat.phredEncoding shouldBe Sanger
  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("nucleocount"), singleThreaded = true, dependsOnGroups = Array("phredscore"))
  def testEncodingNucleotideCount(fqMock: FastqReader) = {

    val seqstat = SeqStat
    nucleotideHistoMap('N') shouldEqual 5
    nucleotideHistoMap('A') shouldEqual 5
    nucleotideHistoMap('C') shouldEqual 5
    nucleotideHistoMap('T') shouldEqual 5
    nucleotideHistoMap('G') shouldEqual 5
  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("basehistogram"), singleThreaded = true, dependsOnGroups = Array("nucleocount"))
  def testEncodingBaseHistogram(fqMock: FastqReader) = {

    val seqstat = SeqStat
    baseQualHistogram(40) shouldEqual 5
    baseQualHistogram(39) shouldEqual 5
    baseQualHistogram(34) shouldEqual 5
    baseQualHistogram(33) shouldEqual 5
    baseQualHistogram.head shouldEqual 5
  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("report"), singleThreaded = true, dependsOnGroups = Array("basehistogram"))
  def testReportOutputScheme(fqMock: FastqReader) = {
    when(fqMock.getFile) thenReturn new File("/tmp/test.fq")
    when(fqMock.iterator) thenReturn recordsOver("1", "2", "3", "4", "5")
    val seqstat = SeqStat
    seqstat.seqStat(fqMock)
    seqstat.summarize()

    val report = seqstat.reportMap(fqMock.getFile)
    report should contain key "files"
    report should contain key "stats"

  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("check_readstats"), singleThreaded = true, dependsOnGroups = Array("report"))
  def testReadStatsObject(fqMock: FastqReader) = {
    when(fqMock.getFile) thenReturn new File("/tmp/test.fq")
    when(fqMock.iterator) thenReturn recordsOver("1", "2", "3", "4", "5")
    val seqstat = SeqStat

    // the histogram should store the lenght==0 value also, for example sequence length 5 is size 6.
    // please note that we already loaded the dataset twice in seqstat. (seqstat.Seqstat is called 2 times in previous steps)
    seqstat.readStats.lengths(5) shouldBe 10
    seqstat.readStats.lengths.length shouldBe 6

    seqstat.readStats.nucs.sum shouldBe 50
    seqstat.readStats.withN shouldBe 10
  }

  @Test def testArgsMinimum() = {
    val args = Array(
      "-i", resourcePath("/paired01a.fq"))
    val parsed = parseArgs(args)
    parsed.fastq shouldBe resourceFile("/paired01a.fq")
  }

  // TODO: Shared state here. Calling main changes the state, which causes other tests to fail
}