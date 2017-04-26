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

import htsjdk.samtools.fastq.{AsyncFastqWriter, FastqReader, FastqRecord}
import org.mockito.Mockito.{inOrder => inOrd, when}
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{DataProvider, Test}

import scala.collection.JavaConverters._

class FastqSyncTest extends TestNGSuite with MockitoSugar with Matchers {

  import FastqSync._

  private def resourceFile(p: String): File =
    new File(resourcePath(p))

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource(p).toURI).toString

  // Helper functions to create iterator over FastqRecords given its IDs as Ints
  private def recordsOver(ids: String*): java.util.Iterator[FastqRecord] =
    ids
      .map(x => new FastqRecord(x, "A", "", "H"))
      .toIterator
      .asJava

  @DataProvider(name = "mockProvider")
  def mockProvider() =
    Array(
      Array(mock[FastqReader],
            mock[FastqReader],
            mock[FastqReader],
            mock[AsyncFastqWriter],
            mock[AsyncFastqWriter])
    )

  @Test(dataProvider = "mockProvider")
  def testDefault(refMock: FastqReader,
                  aMock: FastqReader,
                  bMock: FastqReader,
                  aOutMock: AsyncFastqWriter,
                  bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")
    val obs = inOrd(aOutMock, bOutMock)
    val exp = recordsOver("1", "2", "3").asScala.toSeq

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(exp.head)
    obs.verify(bOutMock).write(exp.head)

    obs.verify(aOutMock).write(exp(1))
    obs.verify(bOutMock).write(exp(1))

    obs.verify(aOutMock).write(exp(2))
    obs.verify(bOutMock).write(exp(2))

    numDiscard1 shouldBe 0
    numDiscard2 shouldBe 0
    numKept shouldBe 3
  }

  @Test(dataProvider = "mockProvider")
  def testRefTooShort(refMock: FastqReader,
                      aMock: FastqReader,
                      bMock: FastqReader,
                      aOutMock: AsyncFastqWriter,
                      bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1", "2")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")

    val thrown = intercept[NoSuchElementException] {
      syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)
    }
    thrown.getMessage should ===("Reference record stream shorter than expected")
  }

  @Test(dataProvider = "mockProvider")
  def testSeqAEmpty(refMock: FastqReader,
                    aMock: FastqReader,
                    bMock: FastqReader,
                    aOutMock: AsyncFastqWriter,
                    bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver()
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    numDiscard1 shouldBe 0
    numDiscard2 shouldBe 3
    numKept shouldBe 0
  }

  @Test(dataProvider = "mockProvider")
  def testSeqBEmpty(refMock: FastqReader,
                    aMock: FastqReader,
                    bMock: FastqReader,
                    aOutMock: AsyncFastqWriter,
                    bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver()

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    numDiscard1 shouldBe 3
    numDiscard2 shouldBe 0
    numKept shouldBe 0
  }

  @Test(dataProvider = "mockProvider")
  def testSeqAShorter(refMock: FastqReader,
                      aMock: FastqReader,
                      bMock: FastqReader,
                      aOutMock: AsyncFastqWriter,
                      bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2", "3")
    val obs = inOrd(aOutMock, bOutMock)
    val exp = recordsOver("1", "2", "3").asScala.toSeq

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    // exp(0) is discarded by syncFastq
    obs.verify(aOutMock).write(exp(1))
    obs.verify(bOutMock).write(exp(1))

    obs.verify(aOutMock).write(exp(2))
    obs.verify(bOutMock).write(exp(2))

    numDiscard1 shouldBe 0
    numDiscard2 shouldBe 1
    numKept shouldBe 2
  }

  @Test(dataProvider = "mockProvider")
  def testSeqBShorter(refMock: FastqReader,
                      aMock: FastqReader,
                      bMock: FastqReader,
                      aOutMock: AsyncFastqWriter,
                      bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "3")
    val obs = inOrd(aOutMock, bOutMock)
    val exp = recordsOver("1", "2", "3").asScala.toSeq

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    // exp(1) is discarded by syncFastq
    obs.verify(aOutMock).write(exp.head)
    obs.verify(bOutMock).write(exp.head)

    obs.verify(aOutMock).write(exp(2))
    obs.verify(bOutMock).write(exp(2))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 0
    numKept shouldBe 2
  }

  @Test(dataProvider = "mockProvider")
  def testSeqABShorter(refMock: FastqReader,
                       aMock: FastqReader,
                       bMock: FastqReader,
                       aOutMock: AsyncFastqWriter,
                       bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1", "2", "3")
    when(aMock.iterator) thenReturn recordsOver("2", "3")
    when(bMock.iterator) thenReturn recordsOver("1", "2")
    val obs = inOrd(aOutMock, bOutMock)
    val exp = recordsOver("1", "2", "3").asScala.toSeq

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    // exp(0) and exp(2) are discarded by syncFastq
    obs.verify(aOutMock).write(exp(1))
    obs.verify(bOutMock).write(exp(1))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 1
  }

  @Test(dataProvider = "mockProvider")
  def testSeqSolexa(refMock: FastqReader,
                    aMock: FastqReader,
                    bMock: FastqReader,
                    aOutMock: AsyncFastqWriter,
                    bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("SOLEXA12_24:6:117:1388:2001/2",
                                                  "SOLEXA12_24:6:96:470:1965/2",
                                                  "SOLEXA12_24:6:35:1209:2037/2")
    when(aMock.iterator) thenReturn recordsOver("SOLEXA12_24:6:96:470:1965/1",
                                                "SOLEXA12_24:6:35:1209:2037/1")
    when(bMock.iterator) thenReturn recordsOver("SOLEXA12_24:6:117:1388:2001/2",
                                                "SOLEXA12_24:6:96:470:1965/2")
    val obs = inOrd(aOutMock, bOutMock)

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(new FastqRecord("SOLEXA12_24:6:96:470:1965/1", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("SOLEXA12_24:6:96:470:1965/2", "A", "", "H"))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 1
  }

  @Test(dataProvider = "mockProvider")
  def testSeqABShorterPairMarkSlash(refMock: FastqReader,
                                    aMock: FastqReader,
                                    bMock: FastqReader,
                                    aOutMock: AsyncFastqWriter,
                                    bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1/1", "2/1", "3/1")
    when(aMock.iterator) thenReturn recordsOver("2/1", "3/1")
    when(bMock.iterator) thenReturn recordsOver("1/2", "2/2")
    val obs = inOrd(aOutMock, bOutMock)

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(new FastqRecord("2/1", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("2/2", "A", "", "H"))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 1
  }

  @Test(dataProvider = "mockProvider")
  def testSeqABShorterPairMarkUnderscore(refMock: FastqReader,
                                         aMock: FastqReader,
                                         bMock: FastqReader,
                                         aOutMock: AsyncFastqWriter,
                                         bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1_1", "2_1", "3_1")
    when(aMock.iterator) thenReturn recordsOver("2_1", "3_1")
    when(bMock.iterator) thenReturn recordsOver("1_2", "2_2")
    val obs = inOrd(aOutMock, bOutMock)

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(new FastqRecord("2_1", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("2_2", "A", "", "H"))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 1
  }

  @Test(dataProvider = "mockProvider")
  def testSeqABShorterWithDescription(refMock: FastqReader,
                                      aMock: FastqReader,
                                      bMock: FastqReader,
                                      aOutMock: AsyncFastqWriter,
                                      bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1 desc1b", "2 desc2b", "3 desc3b")
    when(aMock.iterator) thenReturn recordsOver("2 desc2a", "3 desc3a")
    when(bMock.iterator) thenReturn recordsOver("1 desc1b", "2 desc2b")
    val obs = inOrd(aOutMock, bOutMock)

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(new FastqRecord("2 desc2a", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("2 desc2b", "A", "", "H"))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 1
  }

  @Test(dataProvider = "mockProvider")
  def testComplex(refMock: FastqReader,
                  aMock: FastqReader,
                  bMock: FastqReader,
                  aOutMock: AsyncFastqWriter,
                  bOutMock: AsyncFastqWriter) = {

    when(refMock.iterator) thenReturn recordsOver("1/2 yep",
                                                  "2/2 yep",
                                                  "3/2 yep",
                                                  "4/2 yep",
                                                  "5/2 yep")
    when(aMock.iterator) thenReturn recordsOver("1/1 yep", "2/1 yep", "4/1 yep")
    when(bMock.iterator) thenReturn recordsOver("1/2 yep", "3/2 yep", "4/2 yep")
    val obs = inOrd(aOutMock, bOutMock)

    val (numDiscard1, numDiscard2, numKept) = syncFastq(refMock, aMock, bMock, aOutMock, bOutMock)

    obs.verify(aOutMock).write(new FastqRecord("1/1 yep", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("1/2 yep", "A", "", "H"))

    obs.verify(aOutMock).write(new FastqRecord("4/1 yep", "A", "", "H"))
    obs.verify(bOutMock).write(new FastqRecord("4/2 yep", "A", "", "H"))

    numDiscard1 shouldBe 1
    numDiscard2 shouldBe 1
    numKept shouldBe 2
  }

  @Test def testArgsMinimum() = {
    val args = Array(
      "-r",
      resourcePath("/paired01a.fq"),
      "-i",
      resourcePath("/paired01a.fq"),
      "-j",
      resourcePath("/paired01b.fq"),
      "-o",
      "/tmp/mockout1.fq",
      "-p",
      "/tmp/mockout2.fq"
    )
    val parsed = parseArgs(args)
    parsed.refFastq shouldBe resourceFile("/paired01a.fq")
    parsed.inputFastq1 shouldBe resourceFile("/paired01a.fq")
    parsed.inputFastq2 shouldBe resourceFile("/paired01b.fq")
    parsed.outputFastq1 shouldBe new File("/tmp/mockout1.fq")
    parsed.outputFastq2 shouldBe new File("/tmp/mockout2.fq")
  }
}
