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

import htsjdk.samtools.fastq.{BasicFastqWriter, FastqReader, FastqRecord}
import htsjdk.samtools.util.Interval
import org.mockito.Matchers._
import org.mockito.Mockito.{inOrder => inOrd, times, verify}
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{DataProvider, Test}

class ExtractAlignedFastqTest extends TestNGSuite with MockitoSugar with Matchers {

  import ExtractAlignedFastq._

  private def resourceFile(p: String): File =
    new File(resourcePath(p))

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource(p).toURI).toString

  private def makeInterval(chr: String, start: Int, end: Int): Iterator[Interval] =
    Iterator(new Interval(chr, start, end))

  private def makeInterval(ivs: Iterable[(String, Int, Int)]): Iterator[Interval] =
    ivs.map(x => new Interval(x._1, x._2, x._3)).toIterator

  private def makeRecord(header: String): FastqRecord =
    new FastqRecord(header, "ATGC", "", "HIHI")

  private def makeSingleRecords(headers: String*): Map[String, FastqInput] =
    headers.map(x => (x, (makeRecord(x), None))).toMap

  private def makePairRecords(headers: (String, (String, String))*): Map[String, FastqInput] =
    headers.map(x => (x._1, (makeRecord(x._2._1), Some(makeRecord(x._2._2))))).toMap

  private def makeClue(tName: String, f: File, rName: String): String =
    tName + " on " + f.getName + ", read " + rName + ": "

  @Test def testIntervalStartEnd(): Unit = {
    val obs = makeIntervalFromString(List("chr5:1000-1100")).next()
    val exp = new Interval("chr5", 1000, 1100)
    obs.getContig should ===(exp.getContig)
    obs.getStart should ===(exp.getStart)
    obs.getEnd should ===(exp.getEnd)
  }

  @Test def testIntervalStartEndComma(): Unit = {
    val obs = makeIntervalFromString(List("chr5:1,000-1,100")).next()
    val exp = new Interval("chr5", 1000, 1100)
    obs.getContig should ===(exp.getContig)
    obs.getStart should ===(exp.getStart)
    obs.getEnd should ===(exp.getEnd)
  }

  @Test def testIntervalStartEndDot(): Unit = {
    val obs = makeIntervalFromString(List("chr5:1.000-1.100")).next()
    val exp = new Interval("chr5", 1000, 1100)
    obs.getContig should ===(exp.getContig)
    obs.getStart should ===(exp.getStart)
    obs.getEnd should ===(exp.getEnd)
  }

  @Test def testIntervalStart(): Unit = {
    val obs = makeIntervalFromString(List("chr5:1000")).next()
    val exp = new Interval("chr5", 1000, 1000)
    obs.getContig should ===(exp.getContig)
    obs.getStart should ===(exp.getStart)
    obs.getEnd should ===(exp.getEnd)
  }

  @Test def testIntervalError(): Unit = {
    val thrown = intercept[IllegalArgumentException] {
      makeIntervalFromString(List("chr5:1000-")).next()
    }
    thrown.getMessage should ===("Invalid interval string: chr5:1000-")
  }

  @Test def testMemFuncIntervalError(): Unit = {
    val iv = Iterator(new Interval("chrP", 1, 1000))
    val inAln = resourceFile("/single01.bam")
    val thrown = intercept[IllegalArgumentException] {
      makeMembershipFunction(iv, inAln)
    }
    thrown.getMessage should ===("Chromosome chrP is not found in the alignment file")
  }

  @DataProvider(name = "singleAlnProvider1", parallel = true)
  def singleAlnProvider1(): Array[Array[Object]] = {
    val sFastq1 = makeSingleRecords("r01", "r02", "r03", "r04", "r05")
    val sFastq1Default = sFastq1.keys.map(x => (x, false)).toMap
    val sBam01 = resourceFile("/single01.bam")

    Array(
      Array("adjacent left", makeInterval("chrQ", 30, 49), sBam01, sFastq1, sFastq1Default),
      Array("adjacent right", makeInterval("chrQ", 200, 210), sBam01, sFastq1, sFastq1Default),
      Array("no overlap", makeInterval("chrQ", 220, 230), sBam01, sFastq1, sFastq1Default),
      Array("partial overlap",
            makeInterval("chrQ", 430, 460),
            sBam01,
            sFastq1,
            sFastq1Default.updated("r04", true)),
      Array("enveloped",
            makeInterval("chrQ", 693, 698),
            sBam01,
            sFastq1,
            sFastq1Default.updated("r03", true)),
      Array(
        "partial overlap and enveloped",
        makeInterval(List(("chrQ", 693, 698), ("chrQ", 430, 460))),
        sBam01,
        sFastq1,
        sFastq1Default.updated("r03", true).updated("r04", true)
      )
    )
  }

  @Test(dataProvider = "singleAlnProvider1")
  def testSingleBamDefault(name: String,
                           feats: Iterator[Interval],
                           inAln: File,
                           fastqMap: Map[String, FastqInput],
                           resultMap: Map[String, Boolean]): Unit = {
    require(resultMap.keySet == fastqMap.keySet)
    val memFunc = makeMembershipFunction(feats, inAln)
    for ((key, (rec1, rec2)) <- fastqMap) {
      withClue(makeClue(name, inAln, key)) {
        memFunc(rec1, rec2) shouldBe resultMap(key)
      }
    }
  }

  @DataProvider(name = "singleAlnProvider2", parallel = true)
  def singleAlnProvider2(): Array[Array[Any]] = {
    val sFastq2 = makeSingleRecords("r01", "r02", "r04", "r07", "r06", "r08")
    val sFastq2Default = sFastq2.keys.map(x => (x, false)).toMap
    val sBam02 = resourceFile("/single02.bam")

    Array(
      Array("less than minimum MAPQ",
            makeInterval("chrQ", 830, 890),
            sBam02,
            60,
            sFastq2,
            sFastq2Default),
      Array("greater than minimum MAPQ",
            makeInterval("chrQ", 830, 890),
            sBam02,
            20,
            sFastq2,
            sFastq2Default.updated("r07", true)),
      Array("equal to minimum MAPQ",
            makeInterval("chrQ", 260, 320),
            sBam02,
            30,
            sFastq2,
            sFastq2Default.updated("r01", true))
    )
  }

  @Test(dataProvider = "singleAlnProvider2")
  def testSingleBamMinMapQ(name: String,
                           feats: Iterator[Interval],
                           inAln: File,
                           minMapQ: Int,
                           fastqMap: Map[String, FastqInput],
                           resultMap: Map[String, Boolean]): Unit = {
    require(resultMap.keySet == fastqMap.keySet)
    val memFunc = makeMembershipFunction(feats, inAln, minMapQ)
    for ((key, (rec1, rec2)) <- fastqMap) {
      withClue(makeClue(name, inAln, key)) {
        memFunc(rec1, rec2) shouldBe resultMap(key)
      }
    }
  }
  @DataProvider(name = "pairAlnProvider1", parallel = true)
  def pairAlnProvider1(): Array[Array[Object]] = {
    val pFastq1 = makePairRecords(("r01", ("r01/1", "r01/2")),
                                  ("r02", ("r02/1", "r02/2")),
                                  ("r03", ("r03/1", "r03/2")),
                                  ("r04", ("r04/1", "r04/2")),
                                  ("r05", ("r05/1", "r05/2")))
    val pFastq1Default = pFastq1.keys.map(x => (x, false)).toMap
    val pBam01 = resourceFile("/paired01.bam")

    Array(
      Array("adjacent left", makeInterval("chrQ", 30, 49), pBam01, pFastq1, pFastq1Default),
      Array("adjacent right", makeInterval("chrQ", 200, 210), pBam01, pFastq1, pFastq1Default),
      Array("no overlap", makeInterval("chrQ", 220, 230), pBam01, pFastq1, pFastq1Default),
      Array("partial overlap",
            makeInterval("chrQ", 430, 460),
            pBam01,
            pFastq1,
            pFastq1Default.updated("r04", true)),
      Array("enveloped",
            makeInterval("chrQ", 693, 698),
            pBam01,
            pFastq1,
            pFastq1Default.updated("r03", true)),
      Array("in intron",
            makeInterval("chrQ", 900, 999),
            pBam01,
            pFastq1,
            pFastq1Default.updated("r05", true)),
      Array(
        "partial overlap and enveloped",
        makeInterval(List(("chrQ", 693, 698), ("chrQ", 430, 460))),
        pBam01,
        pFastq1,
        pFastq1Default.updated("r03", true).updated("r04", true)
      )
    )
  }

  @Test(dataProvider = "pairAlnProvider1")
  def testPairBamDefault(name: String,
                         feats: Iterator[Interval],
                         inAln: File,
                         fastqMap: Map[String, FastqInput],
                         resultMap: Map[String, Boolean]): Unit = {
    require(resultMap.keySet == fastqMap.keySet)
    val memFunc = makeMembershipFunction(feats, inAln, commonSuffixLength = 2)
    for ((key, (rec1, rec2)) <- fastqMap) {
      withClue(makeClue(name, inAln, key)) {
        memFunc(rec1, rec2) shouldBe resultMap(key)
      }
    }
  }

  @Test def testWriteSingleFastqDefault(): Unit = {
    val memFunc = (recs: FastqInput) => Set("r01", "r03").contains(fastqId(recs._1))
    val in1 = new FastqReader(resourceFile("/single01.fq"))
    val mo1 = mock[BasicFastqWriter]
    val obs = inOrd(mo1)
    extractReads(memFunc, in1, mo1)
    verify(mo1, times(2)).write(anyObject.asInstanceOf[FastqRecord])
    obs.verify(mo1).write(new FastqRecord("r01", "A", "", "H"))
    obs.verify(mo1).write(new FastqRecord("r03", "G", "", "H"))
  }

  @Test def testWritePairFastqDefault(): Unit = {
    val mockSet = Set("r01/1", "r01/2", "r03/1", "r03/2")
    val memFunc = (recs: FastqInput) =>
      mockSet.contains(fastqId(recs._1)) || mockSet.contains(fastqId(recs._2.get))
    val in1 = new FastqReader(resourceFile("/paired01a.fq"))
    val in2 = new FastqReader(resourceFile("/paired01b.fq"))
    val mo1 = mock[BasicFastqWriter]
    val mo2 = mock[BasicFastqWriter]
    val obs = inOrd(mo1, mo2)
    extractReads(memFunc, in1, mo1, in2, mo2)
    obs.verify(mo1).write(new FastqRecord("r01/1 hello", "A", "", "H"))
    obs.verify(mo2).write(new FastqRecord("r01/2 hello", "T", "", "I"))
    obs.verify(mo1).write(new FastqRecord("r03/1", "G", "", "H"))
    obs.verify(mo2).write(new FastqRecord("r03/2", "C", "", "I"))
    verify(mo1, times(2)).write(anyObject.asInstanceOf[FastqRecord])
    verify(mo2, times(2)).write(anyObject.asInstanceOf[FastqRecord])
  }

  @Test def testArgsMinimum(): Unit = {
    val args = Array(
      "-I",
      resourcePath("/single01.bam"),
      "--interval",
      "chrQ:1-400",
      "-i",
      resourcePath("/single01.fq"),
      "-o",
      "/tmp/tm1.fq"
    )
    val parsed = parseArgs(args)
    parsed.inputBam shouldBe resourceFile("/single01.bam")
    parsed.intervals shouldBe List("chrQ:1-400")
    parsed.inputFastq1 shouldBe resourceFile("/single01.fq")
    parsed.outputFastq1 shouldBe new File("/tmp/tm1.fq")
  }

  @Test def testArgsMaximum(): Unit = {
    val args = Array(
      "-I",
      resourcePath("/paired01.bam"),
      "--interval",
      "chrQ:1-400",
      "--interval",
      "chrP:1000-4000",
      "-i",
      resourcePath("/paired01a.fq"),
      "-j",
      resourcePath("/paired01b.fq"),
      "-o",
      "/tmp/tm1.fq",
      "-p",
      "/tmp/tm2.fq",
      "-s",
      "2",
      "-Q",
      "30"
    )
    val parsed = parseArgs(args)
    parsed.inputBam shouldBe resourceFile("/paired01.bam")
    parsed.intervals shouldBe List("chrQ:1-400", "chrP:1000-4000")
    parsed.inputFastq1 shouldBe resourceFile("/paired01a.fq")
    parsed.inputFastq2.get shouldBe resourceFile("/paired01b.fq")
    parsed.outputFastq1 shouldBe new File("/tmp/tm1.fq")
    parsed.outputFastq2.get shouldBe new File("/tmp/tm2.fq")
    parsed.commonSuffixLength shouldBe 2
    parsed.minMapQ shouldBe 30
  }
}
