package nl.lumc.sasc.biopet.tools

import htsjdk.samtools.util.OverlapDetector
import htsjdk.samtools.{SAMReadGroupRecord, SAMSequenceRecord, SAMLineParser, SAMFileHeader}
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import picard.annotation.Gene

/**
  * Created by pjvan_thof on 1/29/16.
  */
class BaseCounterTest extends TestNGSuite with Matchers {

  import BaseCounter._

  @Test
  def testCountsClass(): Unit = {
    val counts = new Counts
    counts.antiSenseBases shouldBe 0
    counts.antiSenseReads shouldBe 0
    counts.senseBases shouldBe 0
    counts.senseReads shouldBe 0
    counts.totalBases shouldBe 0
    counts.totalReads shouldBe 0

    counts.antiSenseBases = 1
    counts.senseBases = 2
    counts.totalBases shouldBe 3

    counts.antiSenseReads = 1
    counts.senseReads = 2
    counts.totalReads shouldBe 3
  }

  @Test
  def testBamRecordBasesOverlapBlocks(): Unit = {
    val read = BaseCounterTest.lineParser.parseLine("r02\t0\tchrQ\t50\t60\t4M2D4M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001")
    bamRecordBasesOverlap(read, 40, 70) shouldBe 8
    bamRecordBasesOverlap(read, 50, 59) shouldBe 8
    bamRecordBasesOverlap(read, 50, 55) shouldBe 4
    bamRecordBasesOverlap(read, 55, 60) shouldBe 4
    bamRecordBasesOverlap(read, 10, 20) shouldBe 0
    bamRecordBasesOverlap(read, 40, 49) shouldBe 0
    bamRecordBasesOverlap(read, 40, 50) shouldBe 1
    bamRecordBasesOverlap(read, 40, 51) shouldBe 2
    bamRecordBasesOverlap(read, 58, 70) shouldBe 2
    bamRecordBasesOverlap(read, 59, 70) shouldBe 1
    bamRecordBasesOverlap(read, 60, 70) shouldBe 0
  }

    @Test
  def testBamRecordBasesOverlap(): Unit = {
    val read = BaseCounterTest.lineParser.parseLine("r02\t0\tchrQ\t50\t60\t10M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001")
    bamRecordBasesOverlap(read, 40, 70) shouldBe 10
    bamRecordBasesOverlap(read, 50, 59) shouldBe 10
    bamRecordBasesOverlap(read, 50, 55) shouldBe 6
    bamRecordBasesOverlap(read, 55, 60) shouldBe 5
    bamRecordBasesOverlap(read, 10, 20) shouldBe 0
    bamRecordBasesOverlap(read, 40, 49) shouldBe 0
    bamRecordBasesOverlap(read, 40, 50) shouldBe 1
    bamRecordBasesOverlap(read, 40, 51) shouldBe 2
    bamRecordBasesOverlap(read, 58, 70) shouldBe 2
    bamRecordBasesOverlap(read, 59, 70) shouldBe 1
    bamRecordBasesOverlap(read, 60, 70) shouldBe 0

    val counts = new Counts
    bamRecordBasesOverlap(read, 40, 70, counts, true)
    counts.senseBases shouldBe 10
    counts.antiSenseBases shouldBe 0
    counts.senseReads shouldBe 1
    counts.antiSenseReads shouldBe 0

    bamRecordBasesOverlap(read, 50, 54, counts, false)
    counts.senseBases shouldBe 10
    counts.antiSenseBases shouldBe 5
    counts.senseReads shouldBe 1
    counts.antiSenseReads shouldBe 1
  }
}

object BaseCounterTest {
  val lineParser = {
    val header = new SAMFileHeader
    header.addSequence(new SAMSequenceRecord("chrQ", 10000))
    header.addSequence(new SAMSequenceRecord("chrR", 10000))
    header.addReadGroup(new SAMReadGroupRecord("001"))

    new SAMLineParser(header)
  }

  val genes = List()
}