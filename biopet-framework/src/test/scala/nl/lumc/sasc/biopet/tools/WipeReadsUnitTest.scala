/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths
import scala.collection.JavaConverters._

import htsjdk.samtools._
import htsjdk.tribble._
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

class WipeReadsUnitTest extends TestNGSuite with Matchers {

  import WipeReads._

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource(p).toURI).toString

  private val samP: SAMLineParser = {
    val samh = new SAMFileHeader
    samh.addSequence(new SAMSequenceRecord("chrQ", 10000))
    samh.addReadGroup(new SAMReadGroupRecord("001"))
    samh.addReadGroup(new SAMReadGroupRecord("002"))
    new SAMLineParser(samh)
  }

  private def makeSams(raws: String*): Seq[SAMRecord] =
    raws.map(s => samP.parseLine(s))

  private def makeTempBam(): File =
    File.createTempFile("WipeReads", java.util.UUID.randomUUID.toString + ".bam")

  private def makeTempBamIndex(bam: File): File =
    new File(bam.getAbsolutePath.stripSuffix(".bam") + ".bai")

  val bloomSize: Long = 1000
  val bloomFp: Double = 1e-10

  val sBamFile1 = new File(resourcePath("/single01.bam"))
  val sBamRecs1 = makeSams(
    "r02\t0\tchrQ\t50\t60\t10M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001",
    "r01\t16\tchrQ\t190\t60\t10M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001",
    "r01\t16\tchrQ\t290\t60\t10M\t*\t0\t0\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:001",
    "r04\t0\tchrQ\t450\t60\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001",
    "r03\t16\tchrQ\t690\t60\t10M\t*\t0\t0\tCCCCCTTTTT\tHHHHHHHHHH\tRG:Z:001",
    "r05\t0\tchrQ\t890\t60\t5M200N5M\t*\t0\t0\tGATACGATAC\tFEFEFEFEFE\tRG:Z:001",
    "r06\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:001"
  )

  val sBamFile2 = new File(resourcePath("/single02.bam"))
  val sBamRecs2 = makeSams(
    "r02\t0\tchrQ\t50\t60\t10M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001",
    "r01\t16\tchrQ\t190\t30\t10M\t*\t0\t0\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:002",
    "r01\t16\tchrQ\t290\t30\t10M\t*\t0\t0\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:002",
    "r04\t0\tchrQ\t450\t60\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001",
    "r07\t16\tchrQ\t460\t60\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001",
    "r07\t16\tchrQ\t860\t30\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001",
    "r06\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:001",
    "r08\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:002"
  )

  val sBamFile3 = new File(resourcePath("/single03.bam"))
  val sBamFile4 = new File(resourcePath("/single04.bam"))

  val pBamFile1 = new File(resourcePath("/paired01.bam"))
  val pBamRecs1 = makeSams(
    "r02\t99\tchrQ\t50\t60\t10M\t=\t90\t50\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001",
    "r02\t147\tchrQ\t90\t60\t10M\t=\t50\t-50\tATGCATGCAT\tEEFFGGHHII\tRG:Z:001",
    "r01\t163\tchrQ\t150\t60\t10M\t=\t190\t50\tAAAAAGGGGG\tGGGGGGGGGG\tRG:Z:001",
    "r01\t83\tchrQ\t190\t60\t10M\t=\t150\t-50\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:001",
    "r01\t163\tchrQ\t250\t60\t10M\t=\t290\t50\tAAAAAGGGGG\tGGGGGGGGGG\tRG:Z:001",
    "r01\t83\tchrQ\t290\t60\t10M\t=\t250\t-50\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:001",
    "r04\t99\tchrQ\t450\t60\t10M\t=\t490\t50\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001",
    "r04\t147\tchrQ\t490\t60\t10M\t=\t450\t-50\tGCATGCATGC\tEEFFGGHHII\tRG:Z:001",
    "r03\t163\tchrQ\t650\t60\t10M\t=\t690\t50\tTTTTTCCCCC\tHHHHHHHHHH\tRG:Z:001",
    "r03\t83\tchrQ\t690\t60\t10M\t=\t650\t-50\tCCCCCTTTTT\tHHHHHHHHHH\tRG:Z:001",
    "r05\t99\tchrQ\t890\t60\t5M200N5M\t=\t1140\t50\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001",
    "r05\t147\tchrQ\t1140\t60\t10M\t=\t890\t-50\tATGCATGCAT\tEEFFGGHHII\tRG:Z:001",
    "r06\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:001",
    "r06\t4\t*\t0\t0\t*\t*\t0\t0\tGCGCGCGCGC\tHIHIHIHIHI\tRG:Z:001"
  )

  val pBamFile2 = new File(resourcePath("/paired02.bam"))
  val pBamRecs2 = makeSams(
    "r02\t99\tchrQ\t50\t60\t10M\t=\t90\t50\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001",
    "r02\t147\tchrQ\t90\t60\t10M\t=\t50\t-50\tATGCATGCAT\tEEFFGGHHII\tRG:Z:001",
    "r01\t163\tchrQ\t150\t30\t10M\t=\t190\t50\tAAAAAGGGGG\tGGGGGGGGGG\tRG:Z:002",
    "r01\t83\tchrQ\t190\t30\t10M\t=\t150\t-50\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:002",
    "r01\t163\tchrQ\t250\t30\t10M\t=\t290\t50\tAAAAAGGGGG\tGGGGGGGGGG\tRG:Z:002",
    "r01\t83\tchrQ\t290\t30\t10M\t=\t250\t-50\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:002",
    "r04\t99\tchrQ\t450\t60\t10M\t=\t490\t50\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001",
    "r04\t147\tchrQ\t490\t60\t10M\t=\t450\t-50\tGCATGCATGC\tEEFFGGHHII\tRG:Z:001",
    "r06\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:001",
    "r08\t4\t*\t0\t0\t*\t*\t0\t0\tGCGCGCGCGC\tHIHIHIHIHI\tRG:Z:002"
  )

  val pBamFile3 = new File(resourcePath("/paired03.bam"))
  val BedFile1 = new File(resourcePath("/rrna01.bed"))
  val minArgList = List("-I", sBamFile1.toString, "-l", BedFile1.toString, "-o", "mock.bam")

  @Test def testMakeFeatureFromBed() = {
    val intervals: Vector[Feature] = makeFeatureFromFile(BedFile1).toVector
    intervals.length should be(3)
    intervals.head.getChr should ===("chrQ")
    intervals.head.getStart should be(991)
    intervals.head.getEnd should be(1000)
    intervals.last.getChr should ===("chrQ")
    intervals.last.getStart should be(291)
    intervals.last.getEnd should be(320)
  }

  @Test def testSingleBamDefault() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320), // overlaps r01, second hit,
      new BasicFeature("chrQ", 451, 480), // overlaps r04
      new BasicFeature("chrQ", 991, 1000) // overlaps nothing; lies in the spliced region of r05
    )
    // NOTE: while it's possible to have our filter produce false positives
    //       it is highly unlikely in our test cases as we are setting a very low FP rate
    //       and only filling the filter with a few items
    val isFilteredOut = makeFilterOutFunction(intervals, sBamFile1, bloomSize = bloomSize, bloomFp = bloomFp)
    // by default, set elements are SAM record read names
    assert(!isFilteredOut(sBamRecs1(0)))
    assert(isFilteredOut(sBamRecs1(1)))
    assert(isFilteredOut(sBamRecs1(2)))
    assert(isFilteredOut(sBamRecs1(3)))
    assert(!isFilteredOut(sBamRecs1(4)))
    assert(!isFilteredOut(sBamRecs1(5)))
    assert(!isFilteredOut(sBamRecs1(6)))
  }

  @Test def testSingleBamIntervalWithoutChr() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("Q", 291, 320),
      new BasicFeature("chrQ", 451, 480),
      new BasicFeature("P", 191, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBamFile1, bloomSize = bloomSize, bloomFp = bloomFp)
    assert(!isFilteredOut(sBamRecs1(0)))
    assert(isFilteredOut(sBamRecs1(1)))
    assert(isFilteredOut(sBamRecs1(2)))
    assert(isFilteredOut(sBamRecs1(3)))
    assert(!isFilteredOut(sBamRecs1(4)))
    assert(!isFilteredOut(sBamRecs1(5)))
    assert(!isFilteredOut(sBamRecs1(6)))
  }

  @Test def testSingleBamDefaultPartialExonOverlap() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 881, 1000) // overlaps first exon of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBamFile1, bloomSize = bloomSize, bloomFp = bloomFp)
    assert(!isFilteredOut(sBamRecs1(0)))
    assert(!isFilteredOut(sBamRecs1(1)))
    assert(!isFilteredOut(sBamRecs1(2)))
    assert(!isFilteredOut(sBamRecs1(3)))
    assert(!isFilteredOut(sBamRecs1(4)))
    assert(isFilteredOut(sBamRecs1(5)))
    assert(!isFilteredOut(sBamRecs1(6)))
  }

  @Test def testSingleBamDefaultNoExonOverlap() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrP", 881, 1000),
      new BasicFeature("chrQ", 900, 920)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBamFile1, bloomSize = bloomSize, bloomFp = bloomFp)
    assert(!isFilteredOut(sBamRecs1(0)))
    assert(!isFilteredOut(sBamRecs1(1)))
    assert(!isFilteredOut(sBamRecs1(2)))
    assert(!isFilteredOut(sBamRecs1(3)))
    assert(!isFilteredOut(sBamRecs1(4)))
    assert(!isFilteredOut(sBamRecs1(5)))
    assert(!isFilteredOut(sBamRecs1(5)))
    assert(!isFilteredOut(sBamRecs1(6)))
  }

  @Test def testSingleBamFilterOutMultiNotSet() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320), // overlaps r01, second hit,
      new BasicFeature("chrQ", 451, 480), // overlaps r04
      new BasicFeature("chrQ", 991, 1000) // overlaps nothing; lies in the spliced region of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBamFile1, bloomSize = bloomSize, bloomFp = bloomFp,
      filterOutMulti = false)
    assert(!isFilteredOut(sBamRecs1(0)))
    assert(!isFilteredOut(sBamRecs1(1)))
    assert(isFilteredOut(sBamRecs1(2)))
    assert(isFilteredOut(sBamRecs1(3)))
    assert(!isFilteredOut(sBamRecs1(4)))
    assert(!isFilteredOut(sBamRecs1(5)))
    assert(!isFilteredOut(sBamRecs1(6)))
  }

  @Test def testSingleBamFilterMinMapQ() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320),
      new BasicFeature("chrQ", 451, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBamFile2, bloomSize = bloomSize, bloomFp = bloomFp,
      minMapQ = 60)
    assert(!isFilteredOut(sBamRecs2(0)))
    // r01 is not in since it is below the MAPQ threshold
    assert(!isFilteredOut(sBamRecs2(1)))
    assert(!isFilteredOut(sBamRecs2(2)))
    assert(isFilteredOut(sBamRecs2(3)))
    assert(isFilteredOut(sBamRecs2(4)))
    assert(isFilteredOut(sBamRecs2(5)))
    assert(!isFilteredOut(sBamRecs2(6)))
    assert(!isFilteredOut(sBamRecs2(7)))
  }

  @Test def testSingleBamFilterMinMapQFilterOutMultiNotSet() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320),
      new BasicFeature("chrQ", 451, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBamFile2, bloomSize = bloomSize, bloomFp = bloomFp,
      minMapQ = 60, filterOutMulti = false)
    assert(!isFilteredOut(sBamRecs2(0)))
    assert(!isFilteredOut(sBamRecs2(1)))
    // this r01 is not in since it is below the MAPQ threshold
    assert(!isFilteredOut(sBamRecs2(2)))
    assert(isFilteredOut(sBamRecs2(3)))
    assert(isFilteredOut(sBamRecs2(4)))
    // this r07 is not in since filterOuMulti is false
    assert(!isFilteredOut(sBamRecs2(5)))
    assert(!isFilteredOut(sBamRecs2(6)))
    assert(!isFilteredOut(sBamRecs2(7)))
  }

  @Test def testSingleBamFilterReadGroupIDs() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320),
      new BasicFeature("chrQ", 451, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBamFile2, bloomSize = bloomSize, bloomFp = bloomFp,
      readGroupIds = Set("002", "003"))
    assert(!isFilteredOut(sBamRecs2(0)))
    // only r01 is in the set since it is RG 002
    assert(isFilteredOut(sBamRecs2(1)))
    assert(isFilteredOut(sBamRecs2(2)))
    assert(!isFilteredOut(sBamRecs2(3)))
    assert(!isFilteredOut(sBamRecs2(4)))
    assert(!isFilteredOut(sBamRecs2(5)))
    assert(!isFilteredOut(sBamRecs2(6)))
    assert(!isFilteredOut(sBamRecs2(7)))
  }

  @Test def testPairBamDefault() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320), // overlaps r01, second hit,
      new BasicFeature("chrQ", 451, 480), // overlaps r04
      new BasicFeature("chrQ", 991, 1000) // overlaps nothing; lies in the spliced region of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pBamFile1, bloomSize = bloomSize, bloomFp = bloomFp)
    assert(!isFilteredOut(pBamRecs1(0)))
    assert(!isFilteredOut(pBamRecs1(1)))
    assert(isFilteredOut(pBamRecs1(2)))
    assert(isFilteredOut(pBamRecs1(3)))
    assert(isFilteredOut(pBamRecs1(4)))
    assert(isFilteredOut(pBamRecs1(5)))
    assert(isFilteredOut(pBamRecs1(6)))
    assert(isFilteredOut(pBamRecs1(7)))
    assert(!isFilteredOut(pBamRecs1(8)))
    assert(!isFilteredOut(pBamRecs1(9)))
    assert(!isFilteredOut(pBamRecs1(10)))
    assert(!isFilteredOut(pBamRecs1(11)))
    assert(!isFilteredOut(pBamRecs1(12)))
    assert(!isFilteredOut(pBamRecs1(13)))
  }

  @Test def testPairBamPartialExonOverlap() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 891, 1000)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pBamFile1, bloomSize = bloomSize, bloomFp = bloomFp)
    assert(!isFilteredOut(pBamRecs1(0)))
    assert(!isFilteredOut(pBamRecs1(1)))
    assert(!isFilteredOut(pBamRecs1(2)))
    assert(!isFilteredOut(pBamRecs1(3)))
    assert(!isFilteredOut(pBamRecs1(4)))
    assert(!isFilteredOut(pBamRecs1(5)))
    assert(!isFilteredOut(pBamRecs1(6)))
    assert(!isFilteredOut(pBamRecs1(7)))
    assert(!isFilteredOut(pBamRecs1(8)))
    assert(!isFilteredOut(pBamRecs1(9)))
    assert(isFilteredOut(pBamRecs1(10)))
    assert(isFilteredOut(pBamRecs1(11)))
    assert(!isFilteredOut(pBamRecs1(12)))
    assert(!isFilteredOut(pBamRecs1(13)))
  }

  @Test def testPairBamFilterOutMultiNotSet() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320), // overlaps r01, second hit,
      new BasicFeature("chrQ", 451, 480), // overlaps r04
      new BasicFeature("chrQ", 991, 1000) // overlaps nothing; lies in the spliced region of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pBamFile1, bloomSize = bloomSize, bloomFp = bloomFp,
      filterOutMulti = false)
    assert(!isFilteredOut(pBamRecs1(0)))
    assert(!isFilteredOut(pBamRecs1(1)))
    assert(!isFilteredOut(pBamRecs1(2)))
    assert(!isFilteredOut(pBamRecs1(3)))
    assert(isFilteredOut(pBamRecs1(4)))
    assert(isFilteredOut(pBamRecs1(5)))
    assert(isFilteredOut(pBamRecs1(6)))
    assert(isFilteredOut(pBamRecs1(7)))
    assert(!isFilteredOut(pBamRecs1(8)))
    assert(!isFilteredOut(pBamRecs1(9)))
    assert(!isFilteredOut(pBamRecs1(10)))
    assert(!isFilteredOut(pBamRecs1(11)))
    assert(!isFilteredOut(pBamRecs1(12)))
    assert(!isFilteredOut(pBamRecs1(13)))
  }

  @Test def testPairBamFilterMinMapQ() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320),
      new BasicFeature("chrQ", 451, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pBamFile2, bloomSize = bloomSize, bloomFp = bloomFp,
      minMapQ = 60)
    // r01 is not in since it is below the MAPQ threshold
    assert(!isFilteredOut(pBamRecs2(0)))
    assert(!isFilteredOut(pBamRecs2(1)))
    assert(!isFilteredOut(pBamRecs2(2)))
    assert(!isFilteredOut(pBamRecs2(3)))
    assert(!isFilteredOut(pBamRecs2(4)))
    assert(!isFilteredOut(pBamRecs2(5)))
    assert(isFilteredOut(pBamRecs2(6)))
    assert(isFilteredOut(pBamRecs2(7)))
    assert(!isFilteredOut(pBamRecs2(8)))
    assert(!isFilteredOut(pBamRecs2(9)))
  }

  @Test def testPairBamFilterReadGroupIDs() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320),
      new BasicFeature("chrQ", 451, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pBamFile2, bloomSize = bloomSize, bloomFp = bloomFp,
      readGroupIds = Set("002", "003"))
    // only r01 is in the set since it is RG 002
    assert(!isFilteredOut(pBamRecs2(0)))
    assert(!isFilteredOut(pBamRecs2(1)))
    assert(isFilteredOut(pBamRecs2(2)))
    assert(isFilteredOut(pBamRecs2(3)))
    assert(isFilteredOut(pBamRecs2(4)))
    assert(isFilteredOut(pBamRecs2(5)))
    assert(!isFilteredOut(pBamRecs2(6)))
    assert(!isFilteredOut(pBamRecs2(7)))
    assert(!isFilteredOut(pBamRecs2(8)))
    assert(!isFilteredOut(pBamRecs2(9)))
  }

  @Test def testWriteSingleBamDefault() = {
    val mockFilterOutFunc = (r: SAMRecord) => Set("r03", "r04", "r05").contains(r.getReadName)
    val outBam = makeTempBam()
    val outBamIndex = makeTempBamIndex(outBam)
    outBam.deleteOnExit()
    outBamIndex.deleteOnExit()

    val stdout = new java.io.ByteArrayOutputStream
    Console.withOut(stdout) {
      writeFilteredBam(mockFilterOutFunc, sBamFile1, outBam)
    }
    stdout.toString should ===(
      "input_bam\toutput_bam\tcount_included\tcount_excluded\n%s\t%s\t%d\t%d\n"
        .format(sBamFile1.getName, outBam.getName, 4, 3)
    )

    val exp = new SAMFileReader(sBamFile3).asScala
    val obs = new SAMFileReader(outBam).asScala
    for ((e, o) <- exp.zip(obs))
      e.getSAMString should ===(o.getSAMString)
    outBam should be('exists)
    outBamIndex should be('exists)
  }

  @Test def testWriteSingleBamAndFilteredBAM() = {
    val mockFilterOutFunc = (r: SAMRecord) => Set("r03", "r04", "r05").contains(r.getReadName)
    val outBam = makeTempBam()
    val outBamIndex = makeTempBamIndex(outBam)
    outBam.deleteOnExit()
    outBamIndex.deleteOnExit()
    val filteredOutBam = makeTempBam()
    val filteredOutBamIndex = makeTempBamIndex(filteredOutBam)
    filteredOutBam.deleteOnExit()
    filteredOutBamIndex.deleteOnExit()

    val stdout = new java.io.ByteArrayOutputStream
    Console.withOut(stdout) {
      writeFilteredBam(mockFilterOutFunc, sBamFile1, outBam, filteredOutBam = filteredOutBam)
    }
    stdout.toString should ===(
      "input_bam\toutput_bam\tcount_included\tcount_excluded\n%s\t%s\t%d\t%d\n"
        .format(sBamFile1.getName, outBam.getName, 4, 3)
    )

    val exp = new SAMFileReader(sBamFile4).asScala
    val obs = new SAMFileReader(filteredOutBam).asScala
    for ((e, o) <- exp.zip(obs))
      e.getSAMString should ===(o.getSAMString)
    outBam should be('exists)
    outBamIndex should be('exists)
    filteredOutBam should be('exists)
    filteredOutBamIndex should be('exists)
  }

  @Test def testWritePairBamDefault() = {
    val mockFilterOutFunc = (r: SAMRecord) => Set("r03", "r04", "r05").contains(r.getReadName)
    val outBam = makeTempBam()
    val outBamIndex = makeTempBamIndex(outBam)
    outBam.deleteOnExit()
    outBamIndex.deleteOnExit()

    val stdout = new java.io.ByteArrayOutputStream
    Console.withOut(stdout) {
      writeFilteredBam(mockFilterOutFunc, pBamFile1, outBam)
    }
    stdout.toString should ===(
      "input_bam\toutput_bam\tcount_included\tcount_excluded\n%s\t%s\t%d\t%d\n"
        .format(pBamFile1.getName, outBam.getName, 8, 6)
    )
    val exp = new SAMFileReader(pBamFile3).asScala
    val obs = new SAMFileReader(outBam).asScala
    for ((e, o) <- exp.zip(obs))
      e.getSAMString should ===(o.getSAMString)
    outBam should be('exists)
    outBamIndex should be('exists)
  }
}