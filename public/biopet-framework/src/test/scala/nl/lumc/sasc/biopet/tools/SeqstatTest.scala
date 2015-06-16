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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
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

class SeqstatTest extends TestNGSuite with MockitoSugar with Matchers {

  import nl.lumc.sasc.biopet.tools.Seqstat._
  import nl.lumc.sasc.biopet.tools.FqEncoding._

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
    seqstat.summarize()

    seqstat.phredEncoding shouldBe Sanger
  }

  @Test(dataProvider = "mockReaderProvider", groups = Array("nucleocount"), singleThreaded = true, dependsOnGroups = Array("phredscore"))
  def testEncodingNucleotideCount(fqMock: FastqReader) = {

    val seqstat = Seqstat
    nucleotideHistoMap('N') shouldEqual 5
    nucleotideHistoMap('A') shouldEqual 5
    nucleotideHistoMap('C') shouldEqual 5
    nucleotideHistoMap('T') shouldEqual 5
    nucleotideHistoMap('G') shouldEqual 5
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