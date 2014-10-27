/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ DataProvider, Test }

import htsjdk.samtools.fastq.FastqRecord
import htsjdk.tribble._

class ExtractAlignedFastqUnitTest extends TestNGSuite with Matchers {

  import ExtractAlignedFastq._

  private def resourceFile(p: String): File =
    new File(Paths.get(getClass.getResource(p).toURI).toString)

  private def makeFeature(chr: String, start: Int, end: Int): Feature =
    new BasicFeature(chr, start ,end)

  private def makeRecord(header: String): FastqRecord =
    new FastqRecord(header, "ATGC", "", "HIHI")

  private def makeSingleRecords(headers: String*): Map[String, FastqPair] =
    headers.map(x => (x, (makeRecord(x), null))).toMap

  private def makePairRecords(headers: (String, (String, String))*): Map[String, FastqPair] =
    headers.map(x => (x._1, (makeRecord(x._2._1), makeRecord(x._2._2)))).toMap

  private def makeClue(tName: String, f: File, rName: String): String =
    tName + " on " + f.getName + ", read " + rName + ": "

  @Test def testIntervalStartEnd() = {
    val obs = makeFeatureFromString(List("chr5:1000-1100")).next()
    val exp = new BasicFeature("chr5", 1000, 1100)
    obs.getChr should === (exp.getChr)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalStartEndComma() = {
    val obs = makeFeatureFromString(List("chr5:1,000-1,100")).next()
    val exp = new BasicFeature("chr5", 1000, 1100)
    obs.getChr should === (exp.getChr)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalStartEndDot() = {
    val obs = makeFeatureFromString(List("chr5:1.000-1.100")).next()
    val exp = new BasicFeature("chr5", 1000, 1100)
    obs.getChr should === (exp.getChr)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalStart() = {
    val obs = makeFeatureFromString(List("chr5:1000")).next()
    val exp = new BasicFeature("chr5", 1000, 1000)
    obs.getChr should === (exp.getChr)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalError() =
    intercept[IllegalArgumentException] {
      makeFeatureFromString(List("chr5:1000-")).next()
    }

  @DataProvider(name = "singleAlnProvider1", parallel = true)
  def singleAlnProvider1() = {
    val sFastq1 = makeSingleRecords("r01", "r02", "r03", "r04", "r05")
    val sFastq1Default = sFastq1.keys.map(x => (x, false)).toMap
    val sBam01 = resourceFile("/single01.bam")

    Array(
      Array("adjacent left",
        makeFeature("chrQ", 30, 49), sBam01, sFastq1, sFastq1Default),
      Array("adjacent right",
        makeFeature("chrQ", 200, 210), sBam01, sFastq1, sFastq1Default),
      Array("no overlap",
        makeFeature("chrQ", 220, 230), sBam01, sFastq1, sFastq1Default),
      Array("partial overlap",
        makeFeature("chrQ", 430, 460), sBam01, sFastq1, sFastq1Default.updated("r04", true)),
      Array("enveloped",
        makeFeature("chrQ", 693, 698), sBam01, sFastq1, sFastq1Default.updated("r03", true))
    )
  }

  @Test(dataProvider = "singleAlnProvider1")
  def testSingleBamDefault(name: String, feat: Feature, inAln: File,
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
        makeFeature("chrQ", 830, 890), sBam02, 60, sFastq2, sFastq2Default),
      Array("greater than minimum MAPQ",
        makeFeature("chrQ", 830, 890), sBam02, 20, sFastq2, sFastq2Default.updated("r07", true)),
      Array("equal to minimum MAPQ",
        makeFeature("chrQ", 260, 320), sBam02, 30, sFastq2, sFastq2Default.updated("r01", true))
    )
  }

  @Test(dataProvider = "singleAlnProvider2")
  def testSingleBamMinMapQ(name: String, feat: Feature, inAln: File, minMapQ: Int,
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
        makeFeature("chrQ", 30, 49), pBam01, pFastq1, pFastq1Default),
      Array("adjacent right",
        makeFeature("chrQ", 200, 210), pBam01, pFastq1, pFastq1Default),
      Array("no overlap",
        makeFeature("chrQ", 220, 230), pBam01, pFastq1, pFastq1Default),
      Array("partial overlap",
        makeFeature("chrQ", 430, 460), pBam01, pFastq1, pFastq1Default.updated("r04", true)),
      Array("enveloped",
        makeFeature("chrQ", 693, 698), pBam01, pFastq1, pFastq1Default.updated("r03", true)),
      Array("in intron",
        makeFeature("chrQ", 900, 999), pBam01, pFastq1, pFastq1Default.updated("r05", true))
    )
  }

  @Test(dataProvider = "pairAlnProvider1")
  def testPairBamDefault(name: String, feat: Feature, inAln: File,
                         fastqMap: Map[String, FastqPair], resultMap: Map[String, Boolean]) = {
    require(resultMap.keySet == fastqMap.keySet)
    val memFunc = makeMembershipFunction(Iterator(feat), inAln, commonSuffixLength = 2)
    for ((key, (rec1, rec2)) <- fastqMap) {
      withClue(makeClue(name, inAln, key)) {
        memFunc(rec1, rec2) shouldBe resultMap(key)
      }
    }
  }
}
