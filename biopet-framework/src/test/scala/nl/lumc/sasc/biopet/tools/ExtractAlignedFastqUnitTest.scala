/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ DataProvider, Test }

import htsjdk.samtools.util.Interval
import htsjdk.samtools.fastq.{ BasicFastqWriter, FastqReader, FastqRecord }

class ExtractAlignedFastqUnitTest extends TestNGSuite with MockitoSugar with Matchers {

  import ExtractAlignedFastq._

  private def resourceFile(p: String): File =
    new File(Paths.get(getClass.getResource(p).toURI).toString)

  private def makeInterval(chr: String, start: Int, end: Int): Interval =
    new Interval(chr, start, end)

  private def makeRecord(header: String): FastqRecord =
    new FastqRecord(header, "ATGC", "", "HIHI")

  private def makeSingleRecords(headers: String*): Map[String, FastqPair] =
    headers.map(x => (x, (makeRecord(x), null))).toMap

  private def makePairRecords(headers: (String, (String, String))*): Map[String, FastqPair] =
    headers.map(x => (x._1, (makeRecord(x._2._1), makeRecord(x._2._2)))).toMap

  private def makeClue(tName: String, f: File, rName: String): String =
    tName + " on " + f.getName + ", read " + rName + ": "

  @Test def testIntervalStartEnd() = {
    val obs = makeIntervalFromString(List("chr5:1000-1100")).next()
    val exp = new Interval("chr5", 1000, 1100)
    obs.getSequence should === (exp.getSequence)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalStartEndComma() = {
    val obs = makeIntervalFromString(List("chr5:1,000-1,100")).next()
    val exp = new Interval("chr5", 1000, 1100)
    obs.getSequence should === (exp.getSequence)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalStartEndDot() = {
    val obs = makeIntervalFromString(List("chr5:1.000-1.100")).next()
    val exp = new Interval("chr5", 1000, 1100)
    obs.getSequence should === (exp.getSequence)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalStart() = {
    val obs = makeIntervalFromString(List("chr5:1000")).next()
    val exp = new Interval("chr5", 1000, 1000)
    obs.getSequence should === (exp.getSequence)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalError() = {
    val thrown = intercept[IllegalArgumentException] {
      makeIntervalFromString(List("chr5:1000-")).next()
    }
    thrown.getMessage should === ("Invalid interval string: chr5:1000-")
  }

  @Test def testMemFuncIntervalError() = {
    val iv = Iterator(new Interval("chrP", 1, 1000))
    val inAln = resourceFile("/single01.bam")
    val thrown = intercept[IllegalArgumentException] {
      makeMembershipFunction(iv, inAln)
    }
    thrown.getMessage should === ("Chromosome chrP is not found in the alignment file")
  }

  @DataProvider(name = "singleAlnProvider1", parallel = true)
  def singleAlnProvider1() = {
    val sFastq1 = makeSingleRecords("r01", "r02", "r03", "r04", "r05")
    val sFastq1Default = sFastq1.keys.map(x => (x, false)).toMap
    val sBam01 = resourceFile("/single01.bam")

    Array(
      Array("adjacent left",
        makeInterval("chrQ", 30, 49), sBam01, sFastq1, sFastq1Default),
      Array("adjacent right",
        makeInterval("chrQ", 200, 210), sBam01, sFastq1, sFastq1Default),
      Array("no overlap",
        makeInterval("chrQ", 220, 230), sBam01, sFastq1, sFastq1Default),
      Array("partial overlap",
        makeInterval("chrQ", 430, 460), sBam01, sFastq1, sFastq1Default.updated("r04", true)),
      Array("enveloped",
        makeInterval("chrQ", 693, 698), sBam01, sFastq1, sFastq1Default.updated("r03", true))
    )
  }

  @Test(dataProvider = "singleAlnProvider1")
  def testSingleBamDefault(name: String, feat: Interval, inAln: File,
                           fastqMap: Map[String, FastqPair], resultMap: Map[String, Boolean]) = {
    require(resultMap.keySet == fastqMap.keySet)
    val memFunc = makeMembershipFunction(Iterator(feat), inAln)
    for ((key, (rec1, rec2)) <- fastqMap) {
      withClue(makeClue(name, inAln, key)) {
        memFunc(rec1, rec2) shouldBe resultMap(key)
      }
    }
  }

  @DataProvider(name = "singleAlnProvider2", parallel = true)
  def singleAlnProvider2() = {
    val sFastq2 = makeSingleRecords("r01", "r02", "r04", "r07", "r06", "r08")
    val sFastq2Default = sFastq2.keys.map(x => (x, false)).toMap
    val sBam02 = resourceFile("/single02.bam")

    Array(
      Array("less than minimum MAPQ",
        makeInterval("chrQ", 830, 890), sBam02, 60, sFastq2, sFastq2Default),
      Array("greater than minimum MAPQ",
        makeInterval("chrQ", 830, 890), sBam02, 20, sFastq2, sFastq2Default.updated("r07", true)),
      Array("equal to minimum MAPQ",
        makeInterval("chrQ", 260, 320), sBam02, 30, sFastq2, sFastq2Default.updated("r01", true))
    )
  }

  @Test(dataProvider = "singleAlnProvider2")
  def testSingleBamMinMapQ(name: String, feat: Interval, inAln: File, minMapQ: Int,
                           fastqMap: Map[String, FastqPair], resultMap: Map[String, Boolean]) = {
    require(resultMap.keySet == fastqMap.keySet)
    val memFunc = makeMembershipFunction(Iterator(feat), inAln, minMapQ)
    for ((key, (rec1, rec2)) <- fastqMap) {
      withClue(makeClue(name, inAln, key)) {
        memFunc(rec1, rec2) shouldBe resultMap(key)
      }
    }
  }
  @DataProvider(name = "pairAlnProvider1", parallel = true)
  def pairAlnProvider1() = {
    val pFastq1 = makePairRecords(
      ("r01", ("r01/1", "r01/2")),
      ("r02", ("r02/1", "r02/2")),
      ("r03", ("r03/1", "r03/2")),
      ("r04", ("r04/1", "r04/2")),
      ("r05", ("r05/1", "r05/2")))
    val pFastq1Default = pFastq1.keys.map(x => (x, false)).toMap
    val pBam01 = resourceFile("/paired01.bam")

    Array(
      Array("adjacent left",
        makeInterval("chrQ", 30, 49), pBam01, pFastq1, pFastq1Default),
      Array("adjacent right",
        makeInterval("chrQ", 200, 210), pBam01, pFastq1, pFastq1Default),
      Array("no overlap",
        makeInterval("chrQ", 220, 230), pBam01, pFastq1, pFastq1Default),
      Array("partial overlap",
        makeInterval("chrQ", 430, 460), pBam01, pFastq1, pFastq1Default.updated("r04", true)),
      Array("enveloped",
        makeInterval("chrQ", 693, 698), pBam01, pFastq1, pFastq1Default.updated("r03", true)),
      Array("in intron",
        makeInterval("chrQ", 900, 999), pBam01, pFastq1, pFastq1Default.updated("r05", true))
    )
  }

  @Test(dataProvider = "pairAlnProvider1")
  def testPairBamDefault(name: String, feat: Interval, inAln: File,
                         fastqMap: Map[String, FastqPair], resultMap: Map[String, Boolean]) = {
    require(resultMap.keySet == fastqMap.keySet)
    val memFunc = makeMembershipFunction(Iterator(feat), inAln, commonSuffixLength = 2)
    for ((key, (rec1, rec2)) <- fastqMap) {
      withClue(makeClue(name, inAln, key)) {
        memFunc(rec1, rec2) shouldBe resultMap(key)
      }
    }
  }

  @Test def testWriteSingleBamDefault() = {
    val memFunc = (recs: FastqPair) => Set("r01", "r03").contains(recs._1.getReadHeader)
    val in1 = new FastqReader(resourceFile("/single01.fq"))
    val mo1 = mock[BasicFastqWriter]
    selectFastqReads(memFunc, in1, mo1)
    verify(mo1, times(2)).write(anyObject.asInstanceOf[FastqRecord])
    verify(mo1).write(new FastqRecord("r01", "A", "", "H"))
    verify(mo1).write(new FastqRecord("r03", "G", "", "H"))
  }

  @Test def testWritePairBamDefault() = {
    val memFunc = (recs: FastqPair) => Set("r01/1", "r01/2", "r03/1", "r03/2").contains(recs._1.getReadHeader)
    val in1 = new FastqReader(resourceFile("/paired01a.fq"))
    val in2 = new FastqReader(resourceFile("/paired01b.fq"))
    val mo1 = mock[BasicFastqWriter]
    val mo2 = mock[BasicFastqWriter]
    selectFastqReads(memFunc, in1, mo1, in2, mo2)
    verify(mo1, times(2)).write(anyObject.asInstanceOf[FastqRecord])
    verify(mo1).write(new FastqRecord("r01/1", "A", "", "H"))
    verify(mo1).write(new FastqRecord("r03/1", "G", "", "H"))
    verify(mo2, times(2)).write(anyObject.asInstanceOf[FastqRecord])
    verify(mo2).write(new FastqRecord("r01/2", "T", "", "I"))
    verify(mo2).write(new FastqRecord("r03/2", "C", "", "I"))
  }

  @Test def testWriteNoOutputFastq2() = {
    val memFunc: (FastqPair => Boolean) = (recs) => true
    val in1 = mock[FastqReader]
    val in2 = mock[FastqReader]
    val out1 = mock[BasicFastqWriter]
    val thrown = intercept[IllegalArgumentException] {
      selectFastqReads(memFunc, in1, out1, in2)
    }
    thrown.getMessage should === ("Missing output FASTQ 2")
    verify(out1, never).write(anyObject.asInstanceOf[FastqRecord])
  }

  @Test def testWriteNoInputFastq2() = {
    val memFunc: (FastqPair => Boolean) = (recs) => true
    val in1 = mock[FastqReader]
    val out1 = mock[BasicFastqWriter]
    val out2 = mock[BasicFastqWriter]
    val thrown = intercept[IllegalArgumentException] {
      selectFastqReads(memFunc, in1, out1, outputFastq2 = out2)
    }
    thrown.getMessage should === ("Output FASTQ 2 supplied but there is no input FASTQ 2")
    verify(out1, never).write(anyObject.asInstanceOf[FastqRecord])
    verify(out2, never).write(anyObject.asInstanceOf[FastqRecord])
  }
}
