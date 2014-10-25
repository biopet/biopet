/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import htsjdk.samtools.fastq.FastqRecord
import htsjdk.tribble._

class ExtractAlignedFastqUnitTest extends TestNGSuite with Matchers {

  import ExtractAlignedFastq._

  private def resourceFile(p: String): File =
    new File(Paths.get(getClass.getResource(p).toURI).toString)

  private def makeFeatures(features: (String, Int, Int)*): Seq[Feature] =
    features.map(x => new BasicFeature(x._1, x._2, x._3))

  private def makeFastqRecords(raws: (String, String, String, String)*): Seq[FastqRecord] =
    raws.map(x => new FastqRecord(x._1, x._2, x._3, x._4))

  val sBam01 = resourceFile("/single01.bam")
  val pBam01 = resourceFile("/paired01.bam")

  val sFastq1 = makeFastqRecords(
    ("r01", "A", "", "H"),
    ("r02", "T", "", "H"),
    ("r03", "G", "", "H"),
    ("r04", "C", "", "H"),
    ("r05", "AT", "", "HH")
  )

  val pFastq1a = makeFastqRecords(
    ("r01/1", "A", "", "H"),
    ("r02/1", "T", "", "H"),
    ("r03/1", "G", "", "H"),
    ("r04/1", "C", "", "H"),
    ("r05/1", "AT", "", "HH")
  )

  val pFastq1b = makeFastqRecords(
    ("r01/2", "A", "", "H"),
    ("r02/2", "T", "", "H"),
    ("r03/2", "G", "", "H"),
    ("r04/2", "C", "", "H"),
    ("r05/2", "AT", "", "HH")
  )

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

  @Test def testMembershipSingleBamDefault() = {
    val feats = makeFeatures(
      ("chrQ", 30, 49),     // no overlap, adjacent left of read r02
      ("chrQ", 200, 210),   // no overlap, adjacent right of read r01
      ("chrQ", 220, 230),   // no overlap
      ("chrQ", 430, 460),   // overlap, partial on interval and read r04
      ("chrQ", 693, 698))   // overlap, interval enveloped read r03
    val memFunc = makeMembershipFunction(feats, sBam01)
    // r01 is not in the set
    memFunc(sFastq1(0), null) shouldBe false
    // r02 is not in the set
    memFunc(sFastq1(1), null) shouldBe false
    // r03 is in the set
    memFunc(sFastq1(2), null) shouldBe true
    // r04 is in the set
    memFunc(sFastq1(3), null) shouldBe true
    // r05 is not in the set
    memFunc(sFastq1(4), null) shouldBe false
  }

  @Test def testMembershipPairBamDefault() = {
    val feats = makeFeatures(
      ("chrQ", 30, 49),     // no overlap, adjacent left of read r02
      ("chrQ", 200, 210),   // no overlap, adjacent right of read r01
      ("chrQ", 220, 230),   // no overlap, middle of read r01
      ("chrQ", 430, 460),   // overlap, partial on interval and read r04
      ("chrQ", 693, 698),   // overlap, interval enveloped read r03
      ("chrQ", 900, 999))   // enveloped inside read r05 split
    val memFunc = makeMembershipFunction(feats, pBam01, 2)
    memFunc(pFastq1a(0), pFastq1b(0)) shouldBe false
    memFunc(pFastq1a(1), pFastq1b(1)) shouldBe false
    memFunc(pFastq1a(2), pFastq1b(2)) shouldBe true
    memFunc(pFastq1a(3), pFastq1b(3)) shouldBe true
    memFunc(pFastq1a(4), pFastq1b(4)) shouldBe true
  }
}

