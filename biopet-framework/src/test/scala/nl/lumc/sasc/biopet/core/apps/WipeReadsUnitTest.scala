/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.nio.file.Paths
import java.io.{ File, IOException }
import scala.collection.JavaConverters._

import htsjdk.samtools._
import htsjdk.tribble._
import htsjdk.tribble.bed.BEDFeature
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test


class WipeReadsUnitTest extends TestNGSuite with Matchers {

  import WipeReads._

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource(p).toURI).toString

  private lazy val samP: SAMLineParser = {
    val samh = new SAMFileHeader
    samh.addSequence(new SAMSequenceRecord("chrQ", 10000))
    samh.addReadGroup(new SAMReadGroupRecord("001"))
    samh.addReadGroup(new SAMReadGroupRecord("002"))
    new SAMLineParser(samh)
  }

  private def makeSAMs(raws: String*): Seq[SAMRecord] =
    raws.map(s => samP.parseLine(s))

  private def makeTempBAM(): File =
    File.createTempFile("WipeReads", java.util.UUID.randomUUID.toString + ".bam")

  private def makeTempBAMIndex(bam: File): File =
    new File(bam.getAbsolutePath.stripSuffix(".bam") + ".bai")

  val sBAMFile1 = new File(resourcePath("/single01.bam"))
  val sBAMRecs1 = makeSAMs(
    "r02\t0\tchrQ\t50\t60\t10M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001",
    "r01\t16\tchrQ\t190\t60\t10M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001",
    "r01\t16\tchrQ\t290\t60\t10M\t*\t0\t0\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:001",
    "r04\t0\tchrQ\t450\t60\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001",
    "r03\t16\tchrQ\t690\t60\t10M\t*\t0\t0\tCCCCCTTTTT\tHHHHHHHHHH\tRG:Z:001",
    "r05\t0\tchrQ\t890\t60\t5M200N5M\t*\t0\t0\tGATACGATAC\tFEFEFEFEFE\tRG:Z:001",
    "r06\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:001"
  )

  val sBAMFile2 = new File(resourcePath("/single02.bam"))
  val sBAMRecs2 = makeSAMs(
    "r02\t0\tchrQ\t50\t60\t10M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001",
    "r01\t16\tchrQ\t190\t30\t10M\t*\t0\t0\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:002",
    "r01\t16\tchrQ\t290\t30\t10M\t*\t0\t0\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:002",
    "r04\t0\tchrQ\t450\t60\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001",
    "r07\t16\tchrQ\t460\t60\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001",
    "r07\t16\tchrQ\t860\t30\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001",
    "r06\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:001",
    "r08\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:002"
  )

  val sBAMFile3 = new File(resourcePath("/single03.bam"))
  val sBAMFile4 = new File(resourcePath("/single04.bam"))

  val pBAMFile1 = new File(resourcePath("/paired01.bam"))
  val pBAMRecs1 = makeSAMs(
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

  val pBAMFile2 = new File(resourcePath("/paired02.bam"))
  val pBAMRecs2 = makeSAMs(
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

  val pBAMFile3 = new File(resourcePath("/paired03.bam"))
  val BEDFile1 = new File(resourcePath("/rrna01.bed"))
  val minArgList = List("-I", sBAMFile1.toString, "-l", BEDFile1.toString, "-o", "mock.bam")

  @Test def testMakeFeatureFromBED() = {
    val intervals: Vector[Feature] = makeFeatureFromFile(BEDFile1).toVector
    intervals.length should be (3)
    intervals.head.getChr should === ("chrQ")
    intervals.head.getStart should be (991)
    intervals.head.getEnd should be (1000)
    intervals.last.getChr should === ("chrQ")
    intervals.last.getStart should be (291)
    intervals.last.getEnd should be (320)
  }

  @Test def testSingleBAMDefault() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320), // overlaps r01, second hit,
      new BasicFeature("chrQ", 451, 480), // overlaps r04
      new BasicFeature("chrQ", 991, 1000) // overlaps nothing; lies in the spliced region of r05
    )
    // NOTE: while it's possible to have our filter produce false positives
    //       it is highly unlikely in our test cases as we are setting a very low FP rate
    //       and only filling the filter with a few items
    val isFilteredOut = makeFilterOutFunction(intervals, sBAMFile1, bloomSize = 1000, bloomFp = 1e-10)
    // by default, set elements are SAM record read names
    assert(!isFilteredOut(sBAMRecs1(0)))
    assert(isFilteredOut(sBAMRecs1(1)))
    assert(isFilteredOut(sBAMRecs1(2)))
    assert(isFilteredOut(sBAMRecs1(3)))
    assert(!isFilteredOut(sBAMRecs1(4)))
    assert(!isFilteredOut(sBAMRecs1(5)))
    assert(!isFilteredOut(sBAMRecs1(6)))
  }

  @Test def testSingleBAMIntervalWithoutChr() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("Q", 291, 320),
      new BasicFeature("chrQ", 451, 480),
      new BasicFeature("P", 191, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBAMFile1, bloomSize = 1000, bloomFp = 1e-10)
    assert(!isFilteredOut(sBAMRecs1(0)))
    assert(isFilteredOut(sBAMRecs1(1)))
    assert(isFilteredOut(sBAMRecs1(2)))
    assert(isFilteredOut(sBAMRecs1(3)))
    assert(!isFilteredOut(sBAMRecs1(4)))
    assert(!isFilteredOut(sBAMRecs1(5)))
    assert(!isFilteredOut(sBAMRecs1(6)))
  }

  @Test def testSingleBAMDefaultPartialExonOverlap() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 881, 1000) // overlaps first exon of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBAMFile1, bloomSize = 1000, bloomFp = 1e-10)
    assert(!isFilteredOut(sBAMRecs1(0)))
    assert(!isFilteredOut(sBAMRecs1(1)))
    assert(!isFilteredOut(sBAMRecs1(2)))
    assert(!isFilteredOut(sBAMRecs1(3)))
    assert(!isFilteredOut(sBAMRecs1(4)))
    assert(isFilteredOut(sBAMRecs1(5)))
    assert(!isFilteredOut(sBAMRecs1(6)))
  }

  @Test def testSingleBAMDefaultNoExonOverlap() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrP", 881, 1000),
      new BasicFeature("chrQ", 900, 920)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBAMFile1, bloomSize = 1000, bloomFp = 1e-10)
    assert(!isFilteredOut(sBAMRecs1(0)))
    assert(!isFilteredOut(sBAMRecs1(1)))
    assert(!isFilteredOut(sBAMRecs1(2)))
    assert(!isFilteredOut(sBAMRecs1(3)))
    assert(!isFilteredOut(sBAMRecs1(4)))
    assert(!isFilteredOut(sBAMRecs1(5)))
    assert(!isFilteredOut(sBAMRecs1(5)))
    assert(!isFilteredOut(sBAMRecs1(6)))
  }

  @Test def testSingleBAMFilterOutMultiNotSet() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320), // overlaps r01, second hit,
      new BasicFeature("chrQ", 451, 480), // overlaps r04
      new BasicFeature("chrQ", 991, 1000) // overlaps nothing; lies in the spliced region of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBAMFile1, bloomSize = 1000, bloomFp = 1e-10,
      filterOutMulti = false)
    assert(!isFilteredOut(sBAMRecs1(0)))
    assert(!isFilteredOut(sBAMRecs1(1)))
    assert(isFilteredOut(sBAMRecs1(2)))
    assert(isFilteredOut(sBAMRecs1(3)))
    assert(!isFilteredOut(sBAMRecs1(4)))
    assert(!isFilteredOut(sBAMRecs1(5)))
    assert(!isFilteredOut(sBAMRecs1(6)))
  }

  @Test def testSingleBAMFilterMinMapQ() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320),
      new BasicFeature("chrQ", 451, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBAMFile2, bloomSize = 1000, bloomFp = 1e-10,
      minMapQ = 60)
    assert(!isFilteredOut(sBAMRecs2(0)))
    // r01 is not in since it is below the MAPQ threshold
    assert(!isFilteredOut(sBAMRecs2(1)))
    assert(!isFilteredOut(sBAMRecs2(2)))
    assert(isFilteredOut(sBAMRecs2(3)))
    assert(isFilteredOut(sBAMRecs2(4)))
    assert(isFilteredOut(sBAMRecs2(5)))
    assert(!isFilteredOut(sBAMRecs2(6)))
    assert(!isFilteredOut(sBAMRecs2(7)))
  }

  @Test def testSingleBAMFilterMinMapQFilterOutMultiNotSet() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320),
      new BasicFeature("chrQ", 451, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBAMFile2, bloomSize = 1000, bloomFp = 1e-10,
      minMapQ = 60, filterOutMulti = false)
    assert(!isFilteredOut(sBAMRecs2(0)))
    assert(!isFilteredOut(sBAMRecs2(1)))
    // this r01 is not in since it is below the MAPQ threshold
    assert(!isFilteredOut(sBAMRecs2(2)))
    assert(isFilteredOut(sBAMRecs2(3)))
    assert(isFilteredOut(sBAMRecs2(4)))
    // this r07 is not in since filterOuMulti is false
    assert(!isFilteredOut(sBAMRecs2(5)))
    assert(!isFilteredOut(sBAMRecs2(6)))
    assert(!isFilteredOut(sBAMRecs2(7)))
  }

  @Test def testSingleBAMFilterReadGroupIDs() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320),
      new BasicFeature("chrQ", 451, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sBAMFile2, bloomSize = 1000, bloomFp = 1e-10,
      readGroupIDs = Set("002", "003"))
    assert(!isFilteredOut(sBAMRecs2(0)))
    // only r01 is in the set since it is RG 002
    assert(isFilteredOut(sBAMRecs2(1)))
    assert(isFilteredOut(sBAMRecs2(2)))
    assert(!isFilteredOut(sBAMRecs2(3)))
    assert(!isFilteredOut(sBAMRecs2(4)))
    assert(!isFilteredOut(sBAMRecs2(5)))
    assert(!isFilteredOut(sBAMRecs2(6)))
    assert(!isFilteredOut(sBAMRecs2(7)))
  }

  @Test def testPairBAMDefault() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320), // overlaps r01, second hit,
      new BasicFeature("chrQ", 451, 480), // overlaps r04
      new BasicFeature("chrQ", 991, 1000) // overlaps nothing; lies in the spliced region of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pBAMFile1, bloomSize = 1000, bloomFp = 1e-10)
    assert(!isFilteredOut(pBAMRecs1(0)))
    assert(!isFilteredOut(pBAMRecs1(1)))
    assert(isFilteredOut(pBAMRecs1(2)))
    assert(isFilteredOut(pBAMRecs1(3)))
    assert(isFilteredOut(pBAMRecs1(4)))
    assert(isFilteredOut(pBAMRecs1(5)))
    assert(isFilteredOut(pBAMRecs1(6)))
    assert(isFilteredOut(pBAMRecs1(7)))
    assert(!isFilteredOut(pBAMRecs1(8)))
    assert(!isFilteredOut(pBAMRecs1(9)))
    assert(!isFilteredOut(pBAMRecs1(10)))
    assert(!isFilteredOut(pBAMRecs1(11)))
    assert(!isFilteredOut(pBAMRecs1(12)))
    assert(!isFilteredOut(pBAMRecs1(13)))
  }

  @Test def testPairBAMPartialExonOverlap() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 891, 1000)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pBAMFile1, bloomSize = 1000, bloomFp = 1e-10)
    assert(!isFilteredOut(pBAMRecs1(0)))
    assert(!isFilteredOut(pBAMRecs1(1)))
    assert(!isFilteredOut(pBAMRecs1(2)))
    assert(!isFilteredOut(pBAMRecs1(3)))
    assert(!isFilteredOut(pBAMRecs1(4)))
    assert(!isFilteredOut(pBAMRecs1(5)))
    assert(!isFilteredOut(pBAMRecs1(6)))
    assert(!isFilteredOut(pBAMRecs1(7)))
    assert(!isFilteredOut(pBAMRecs1(8)))
    assert(!isFilteredOut(pBAMRecs1(9)))
    assert(isFilteredOut(pBAMRecs1(10)))
    assert(isFilteredOut(pBAMRecs1(11)))
    assert(!isFilteredOut(pBAMRecs1(12)))
    assert(!isFilteredOut(pBAMRecs1(13)))
  }

  @Test def testPairBAMFilterOutMultiNotSet() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320), // overlaps r01, second hit,
      new BasicFeature("chrQ", 451, 480), // overlaps r04
      new BasicFeature("chrQ", 991, 1000) // overlaps nothing; lies in the spliced region of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pBAMFile1, bloomSize = 1000, bloomFp = 1e-10,
      filterOutMulti = false)
    assert(!isFilteredOut(pBAMRecs1(0)))
    assert(!isFilteredOut(pBAMRecs1(1)))
    assert(!isFilteredOut(pBAMRecs1(2)))
    assert(!isFilteredOut(pBAMRecs1(3)))
    assert(isFilteredOut(pBAMRecs1(4)))
    assert(isFilteredOut(pBAMRecs1(5)))
    assert(isFilteredOut(pBAMRecs1(6)))
    assert(isFilteredOut(pBAMRecs1(7)))
    assert(!isFilteredOut(pBAMRecs1(8)))
    assert(!isFilteredOut(pBAMRecs1(9)))
    assert(!isFilteredOut(pBAMRecs1(10)))
    assert(!isFilteredOut(pBAMRecs1(11)))
    assert(!isFilteredOut(pBAMRecs1(12)))
    assert(!isFilteredOut(pBAMRecs1(13)))
  }

  @Test def testPairBAMFilterMinMapQ() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320),
      new BasicFeature("chrQ", 451, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pBAMFile2, bloomSize = 1000, bloomFp = 1e-10,
      minMapQ = 60)
    // r01 is not in since it is below the MAPQ threshold
    assert(!isFilteredOut(pBAMRecs2(0)))
    assert(!isFilteredOut(pBAMRecs2(1)))
    assert(!isFilteredOut(pBAMRecs2(2)))
    assert(!isFilteredOut(pBAMRecs2(3)))
    assert(!isFilteredOut(pBAMRecs2(4)))
    assert(!isFilteredOut(pBAMRecs2(5)))
    assert(isFilteredOut(pBAMRecs2(6)))
    assert(isFilteredOut(pBAMRecs2(7)))
    assert(!isFilteredOut(pBAMRecs2(8)))
    assert(!isFilteredOut(pBAMRecs2(9)))
  }

  @Test def testPairBAMFilterReadGroupIDs() = {
    val intervals: Iterator[Feature] = Iterator(
      new BasicFeature("chrQ", 291, 320),
      new BasicFeature("chrQ", 451, 480)
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pBAMFile2, bloomSize = 1000, bloomFp = 1e-10,
      readGroupIDs = Set("002", "003"))
    // only r01 is in the set since it is RG 002
    assert(!isFilteredOut(pBAMRecs2(0)))
    assert(!isFilteredOut(pBAMRecs2(1)))
    assert(isFilteredOut(pBAMRecs2(2)))
    assert(isFilteredOut(pBAMRecs2(3)))
    assert(isFilteredOut(pBAMRecs2(4)))
    assert(isFilteredOut(pBAMRecs2(5)))
    assert(!isFilteredOut(pBAMRecs2(6)))
    assert(!isFilteredOut(pBAMRecs2(7)))
    assert(!isFilteredOut(pBAMRecs2(8)))
    assert(!isFilteredOut(pBAMRecs2(9)))
  }

  @Test def testWriteSingleBAMDefault() = {
    val mockFilterOutFunc = (r: SAMRecord) => Set("r03", "r04", "r05").contains(r.getReadName)
    val outBAM = makeTempBAM()
    val outBAMIndex = makeTempBAMIndex(outBAM)
    outBAM.deleteOnExit()
    outBAMIndex.deleteOnExit()

    val stdout = new java.io.ByteArrayOutputStream
    Console.withOut(stdout) {
      writeFilteredBAM(mockFilterOutFunc, sBAMFile1, outBAM)
    }
    stdout.toString should === (
      "input_bam\toutput_bam\tcount_included\tcount_excluded\n%s\t%s\t%d\t%d\n"
        .format(sBAMFile1.getName, outBAM.getName, 4, 3)
    )

    val exp = new SAMFileReader(sBAMFile3).asScala
    val obs = new SAMFileReader(outBAM).asScala
    for ((e, o) <- exp.zip(obs))
      e.getSAMString should === (o.getSAMString)
    outBAM should be ('exists)
    outBAMIndex should be ('exists)
  }

  @Test def testWriteSingleBAMAndFilteredBAM() = {
    val mockFilterOutFunc = (r: SAMRecord) => Set("r03", "r04", "r05").contains(r.getReadName)
    val outBAM = makeTempBAM()
    val outBAMIndex = makeTempBAMIndex(outBAM)
    outBAM.deleteOnExit()
    outBAMIndex.deleteOnExit()
    val filteredOutBAM = makeTempBAM()
    val filteredOutBAMIndex = makeTempBAMIndex(filteredOutBAM)
    filteredOutBAM.deleteOnExit()
    filteredOutBAMIndex.deleteOnExit()

    val stdout = new java.io.ByteArrayOutputStream
    Console.withOut(stdout) {
      writeFilteredBAM(mockFilterOutFunc, sBAMFile1, outBAM, filteredOutBAM = filteredOutBAM)
    }
    stdout.toString should === (
      "input_bam\toutput_bam\tcount_included\tcount_excluded\n%s\t%s\t%d\t%d\n"
        .format(sBAMFile1.getName, outBAM.getName, 4, 3)
    )

    val exp = new SAMFileReader(sBAMFile4).asScala
    val obs = new SAMFileReader(filteredOutBAM).asScala
    for ((e, o) <- exp.zip(obs))
      e.getSAMString should === (o.getSAMString)
    outBAM should be ('exists)
    outBAMIndex should be ('exists)
    filteredOutBAM should be ('exists)
    filteredOutBAMIndex should be ('exists)
  }

  @Test def testWritePairBAMDefault() = {
    val mockFilterOutFunc = (r: SAMRecord) => Set("r03", "r04", "r05").contains(r.getReadName)
    val outBAM = makeTempBAM()
    val outBAMIndex = makeTempBAMIndex(outBAM)
    outBAM.deleteOnExit()
    outBAMIndex.deleteOnExit()

    val stdout = new java.io.ByteArrayOutputStream
    Console.withOut(stdout) {
      writeFilteredBAM(mockFilterOutFunc, pBAMFile1, outBAM)
    }
    stdout.toString should === (
      "input_bam\toutput_bam\tcount_included\tcount_excluded\n%s\t%s\t%d\t%d\n"
        .format(pBAMFile1.getName, outBAM.getName, 8, 6)
    )
    val exp = new SAMFileReader(pBAMFile3).asScala
    val obs = new SAMFileReader(outBAM).asScala
    for ((e, o) <- exp.zip(obs))
      e.getSAMString should === (o.getSAMString)
    outBAM should be ('exists)
    outBAMIndex should be ('exists)
  }
}