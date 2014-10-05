/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.core.apps

import java.nio.file.Paths
import java.io.{ File, IOException }
import scala.collection.JavaConverters._

import htsjdk.samtools.{ SAMFileReader, SAMRecord }
import org.scalatest.Assertions
import org.testng.annotations.Test


class WipeReadsUnitTest extends Assertions {

  import WipeReads._

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource(p).toURI).toString

  val sbam01 = new File(resourcePath("/single01.bam"))
  val sbam02 = new File(resourcePath("/single02.bam"))
  val sbam03 = new File(resourcePath("/single03.bam"))
  val pbam01 = new File(resourcePath("/paired01.bam"))
  val pbam02 = new File(resourcePath("/paired02.bam"))
  val pbam03 = new File(resourcePath("/paired03.bam"))
  val bed01 = new File(resourcePath("/rrna01.bed"))
  val minArgList = List("-I", sbam01.toString, "-l", bed01.toString, "-o", "mock.bam")

  @Test def rawIntervalBED() = {
    val intervals: Vector[RawInterval] = makeRawIntervalFromFile(bed01).toVector
    assert(intervals.length == 3)
    assert(intervals.head.chrom == "chrQ")
    assert(intervals.head.start == 291)
    assert(intervals.head.end == 320)
    assert(intervals.head.strand == "+")
    assert(intervals.last.chrom == "chrQ")
    assert(intervals.last.start == 991)
    assert(intervals.last.end == 1000)
    assert(intervals.last.strand == "+")
  }

  @Test def testSingleBAMDefault() = {
    val intervals: Iterator[RawInterval] = Iterator(
      RawInterval("chrQ", 291, 320, "+"), // overlaps r01, second hit,
      RawInterval("chrQ", 451, 480, "+"), // overlaps r04
      RawInterval("chrQ", 991, 1000, "+") // overlaps nothing; lies in the spliced region of r05
    )
    // NOTE: while it's possible to have our filter produce false positives
    //       it is highly unlikely in our test cases as we are setting a very low FP rate
    //       and only filling the filter with a few items
    val isFilteredOut = makeFilterOutFunction(intervals, sbam01, bloomSize = 1000, bloomFp = 1e-10)
    // by default, set elements are SAM record read names
    assert(!isFilteredOut("r02"))
    assert(!isFilteredOut("r03"))
    /* TODO: exclude r05 from set since the mapped read does not overlap with our interval
             this is a limitation of htsjdk, as it checks for overlaps with records instead of alignment blocks
    assert(!isFilteredOut("r05"))
     */
    assert(!isFilteredOut("r06"))
    assert(isFilteredOut("r01"))
    assert(isFilteredOut("r04"))
  }

  @Test def testSingleBAMFilterOutMultiNotSet() = {
    val intervals: Iterator[RawInterval] = Iterator(
      RawInterval("chrQ", 291, 320, "+"), // overlaps r01, second hit,
      RawInterval("chrQ", 451, 480, "+"), // overlaps r04
      RawInterval("chrQ", 991, 1000, "+") // overlaps nothing; lies in the spliced region of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sbam01, bloomSize = 1000, bloomFp = 1e-10,
                                              filterOutMulti = false)
    assert(!isFilteredOut("r02\t0\tchrQ\t50\t60\t10M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001\n"))
    assert(!isFilteredOut("r01\t16\tchrQ\t190\t60\t10M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001\n"))
    assert(!isFilteredOut("r03\t16\tchrQ\t690\t60\t10M\t*\t0\t0\tCCCCCTTTTT\tHHHHHHHHHH\tRG:Z:001\n"))
    assert(!isFilteredOut("r06\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:001\n"))
    assert(!isFilteredOut("r06\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:001\n"))
    assert(isFilteredOut("r01\t16\tchrQ\t290\t60\t10M\t*\t0\t0\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:001\n"))
    assert(isFilteredOut("r04\t0\tchrQ\t450\t60\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001\n"))
    /* TODO: exclude r05 from set since the mapped read does not overlap with our interval
             this is a limitation of htsjdk, as it checks for overlaps with records instead of alignment blocks
    assert(!isFilteredOut("r05\t0\tchrQ\t890\t60\t5M200N5M\t*\t0\t0\tGATACGATAC\tFEFEFEFEFE\tRG:Z:001\n"))
     */
  }

  @Test def testSingleBAMFilterMinMapQ() = {
    val intervals: Iterator[RawInterval] = Iterator(
      RawInterval("chrQ", 291, 320, "+"),
      RawInterval("chrQ", 451, 480, "+")
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sbam02, bloomSize = 1000, bloomFp = 1e-10,
                                              minMapQ = 60)
    // r01 is not in since it is below the MAPQ threshold
    assert(!isFilteredOut("r01"))
    assert(!isFilteredOut("r02"))
    assert(!isFilteredOut("r06"))
    assert(!isFilteredOut("r08"))
    assert(isFilteredOut("r04"))
    assert(isFilteredOut("r07"))
  }

  @Test def testSingleBAMFilterMinMapQFilterOutMultiNotSet() = {
    val intervals: Iterator[RawInterval] = Iterator(
      RawInterval("chrQ", 291, 320, "+"),
      RawInterval("chrQ", 451, 480, "+")
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sbam02, bloomSize = 1000, bloomFp = 1e-10,
                                              minMapQ = 60, filterOutMulti = false)
    assert(!isFilteredOut("r02\t0\tchrQ\t50\t60\t10M\t*\t0\t0\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001\n"))
    assert(!isFilteredOut("r01\t16\tchrQ\t190\t30\t10M\t*\t0\t0\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:002\n"))
    // this r01 is not in since it is below the MAPQ threshold
    assert(!isFilteredOut("r01\t16\tchrQ\t290\t30\t10M\t*\t0\t0\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:002\n"))
    assert(!isFilteredOut("r07\t16\tchrQ\t860\t30\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001\n"))
    assert(!isFilteredOut("r06\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:001\n"))
    assert(!isFilteredOut("r08\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:002\n"))
    assert(isFilteredOut("r04\t0\tchrQ\t450\t60\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001\n"))
    // this r07 is not in since filterOuMulti is false
    assert(isFilteredOut("r07\t16\tchrQ\t460\t60\t10M\t*\t0\t0\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001\n"))
  }

  @Test def testSingleBAMFilterReadGroupIDs() = {
    val intervals: Iterator[RawInterval] = Iterator(
      RawInterval("chrQ", 291, 320, "+"),
      RawInterval("chrQ", 451, 480, "+")
    )
    val isFilteredOut = makeFilterOutFunction(intervals, sbam02, bloomSize = 1000, bloomFp = 1e-10,
                                              readGroupIDs = Set("002", "003"))
    assert(!isFilteredOut("r02"))
    assert(!isFilteredOut("r04"))
    assert(!isFilteredOut("r06"))
    assert(!isFilteredOut("r08"))
    // only r01 is in the set since it is RG 002
    assert(isFilteredOut("r01"))
  }

  @Test def testPairBAMDefault() = {
    val intervals: Iterator[RawInterval] = Iterator(
      RawInterval("chrQ", 291, 320, "+"), // overlaps r01, second hit,
      RawInterval("chrQ", 451, 480, "+"), // overlaps r04
      RawInterval("chrQ", 991, 1000, "+") // overlaps nothing; lies in the spliced region of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pbam01, bloomSize = 1000, bloomFp = 1e-10)
    // by default, set elements are SAM record read names
    assert(!isFilteredOut("r02"))
    assert(!isFilteredOut("r03"))
    /* TODO: exclude r05 from set since the mapped read does not overlap with our interval
             this is a limitation of htsjdk, as it checks for overlaps with records instead of alignment blocks
    assert(!isFilteredOut("r05"))
     */
    assert(!isFilteredOut("r06"))
    assert(isFilteredOut("r01"))
    assert(isFilteredOut("r04"))
  }

  @Test def testPairBAMFilterOutMultiNotSet() = {
    val intervals: Iterator[RawInterval] = Iterator(
      RawInterval("chrQ", 291, 320, "+"), // overlaps r01, second hit,
      RawInterval("chrQ", 451, 480, "+"), // overlaps r04
      RawInterval("chrQ", 991, 1000, "+") // overlaps nothing; lies in the spliced region of r05
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pbam01, bloomSize = 1000, bloomFp = 1e-10,
                                              filterOutMulti = false)
    assert(!isFilteredOut("r02\t99\tchrQ\t50\t60\t10M\t=\t90\t50\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001\n"))
    assert(!isFilteredOut("r02\t147\tchrQ\t90\t60\t10M\t=\t50\t-50\tATGCATGCAT\tEEFFGGHHII\tRG:Z:001\n"))
    assert(!isFilteredOut("r01\t163\tchrQ\t150\t60\t10M\t=\t190\t50\tAAAAAGGGGG\tGGGGGGGGGG\tRG:Z:001\n"))
    assert(!isFilteredOut("r01\t83\tchrQ\t190\t60\t10M\t=\t150\t-50\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:001\n"))
    assert(!isFilteredOut("r03\t163\tchrQ\t650\t60\t10M\t=\t690\t50\tTTTTTCCCCC\tHHHHHHHHHH\tRG:Z:001\n"))
    assert(!isFilteredOut("r03\t83\tchrQ\t690\t60\t10M\t=\t650\t-50\tCCCCCTTTTT\tHHHHHHHHHH\tRG:Z:001\n"))
    assert(!isFilteredOut("r06\t4\t*\t0\t0\t*\t*\t0\t0\tATATATATAT\tHIHIHIHIHI\tRG:Z:001\n"))
    assert(!isFilteredOut("r06\t4\t*\t0\t0\t*\t*\t0\t0\tGCGCGCGCGC\tHIHIHIHIHI\tRG:Z:001\n"))
    assert(isFilteredOut("r01\t163\tchrQ\t250\t60\t10M\t=\t290\t50\tAAAAAGGGGG\tGGGGGGGGGG\tRG:Z:001\n"))
    assert(isFilteredOut("r01\t83\tchrQ\t290\t60\t10M\t=\t250\t-50\tGGGGGAAAAA\tGGGGGGGGGG\tRG:Z:001\n"))
    assert(isFilteredOut("r04\t99\tchrQ\t450\t60\t10M\t=\t490\t50\tCGTACGTACG\tEEFFGGHHII\tRG:Z:001\n"))
    assert(isFilteredOut("r04\t147\tchrQ\t490\t60\t10M\t=\t450\t-50\tGCATGCATGC\tEEFFGGHHII\tRG:Z:001\n"))
    /* TODO: exclude r05 from set
    assert(!isFilteredOut("r05\t99\tchrQ\t850\t60\t5M100N5M\t=\t1140\t50\tTACGTACGTA\tEEFFGGHHII\tRG:Z:001\n"))
    assert(!isFilteredOut("r05\t147\tchrQ\t1140\t60\t10M\t=\t850\t-50\tATGCATGCAT\tEEFFGGHHII\tRG:Z:001\n"))
     */
  }

  @Test def testPairBAMFilterMinMapQ() = {
    val intervals: Iterator[RawInterval] = Iterator(
      RawInterval("chrQ", 291, 320, "+"),
      RawInterval("chrQ", 451, 480, "+")
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pbam02, bloomSize = 1000, bloomFp = 1e-10,
                                              minMapQ = 60)
    // r01 is not in since it is below the MAPQ threshold
    assert(!isFilteredOut("r01"))
    assert(!isFilteredOut("r02"))
    assert(!isFilteredOut("r06"))
    assert(!isFilteredOut("r08"))
    assert(isFilteredOut("r04"))
  }

  @Test def testPairBAMFilterReadGroupIDs() = {
    val intervals: Iterator[RawInterval] = Iterator(
      RawInterval("chrQ", 291, 320, "+"),
      RawInterval("chrQ", 451, 480, "+")
    )
    val isFilteredOut = makeFilterOutFunction(intervals, pbam02, bloomSize = 1000, bloomFp = 1e-10,
                                              readGroupIDs = Set("002", "003"))
    assert(!isFilteredOut("r02"))
    assert(!isFilteredOut("r04"))
    assert(!isFilteredOut("r06"))
    assert(!isFilteredOut("r08"))
    // only r01 is in the set since it is RG 002
    assert(isFilteredOut("r01"))
  }

  @Test def testWriteSingleBAMDefault() = {
    val mockFilterOutFunc = (r: SAMRecord) => Set("r03", "r04", "r05").contains(r.getReadName)
    val outBAM = File.createTempFile("WipeReads", java.util.UUID.randomUUID.toString + ".bam")
    val outBAMIndex = new File(outBAM.getAbsolutePath.stripSuffix(".bam") + ".bai")
    outBAM.deleteOnExit()
    outBAMIndex.deleteOnExit()
    writeFilteredBAM(mockFilterOutFunc, sbam01, outBAM)
    val exp = new SAMFileReader(sbam03).asScala
    val obs = new SAMFileReader(outBAM).asScala
    val res = for ( (e,o) <- exp.zip(obs)) yield e.getSAMString === o.getSAMString
    assert(res.reduceLeft(_ && _))
    assert(outBAM.exists)
    assert(outBAMIndex.exists)
  }

  @Test def testWritePairBAMDefault() = {
    val mockFilterOutFunc = (r: SAMRecord) => Set("r03", "r04", "r05").contains(r.getReadName)
    val outBAM = File.createTempFile("WipeReads", java.util.UUID.randomUUID.toString + ".bam")
    val outBAMIndex = new File(outBAM.getAbsolutePath.stripSuffix(".bam") + ".bai")
    outBAM.deleteOnExit()
    outBAMIndex.deleteOnExit()
    writeFilteredBAM(mockFilterOutFunc, pbam01, outBAM)
    val exp = new SAMFileReader(pbam03).asScala
    val obs = new SAMFileReader(outBAM).asScala
    val res = for ( (e,o) <- exp.zip(obs)) yield e.getSAMString === o.getSAMString
    assert(res.reduceLeft(_ && _))
    assert(outBAM.exists)
    assert(outBAMIndex.exists)
  }

  @Test def testOptMinimum() = {
    val opts = parseOption(Map(), minArgList)
    assert(opts.contains("inputBAM"))
    assert(opts.contains("targetRegions"))
    assert(opts.contains("outputBAM"))
  }

  @Test def testOptMissingBAI() = {
    val pathBAM = File.createTempFile("WipeReads", java.util.UUID.randomUUID.toString)
    assert(pathBAM.exists)
    val argList = List(
      "--inputBAM", pathBAM.toPath.toString,
      "--targetRegions", bed01.getPath,
      "--outputBAM", "mock.bam")
    val thrown = intercept[IOException] {
      parseOption(Map(), argList)
    }
    assert(thrown.getMessage === "Index for input BAM file " + pathBAM + " not found")
    pathBAM.deleteOnExit()
  }

  @Test def testOptMissingRegions() = {
    val pathRegion = "/i/dont/exist.bed"
    val argList = List(
      "--inputBAM", sbam01.getPath,
      "--targetRegions", pathRegion,
      "--outputBAM", "mock.bam"
    )
    val thrown = intercept[IOException] {
      parseOption(Map(), argList)
    }
    assert(thrown.getMessage === "Input file " + pathRegion + " not found")
  }

  @Test def testOptUnexpected() = {
    val argList = List("--strand", "sense") ::: minArgList
    val thrown = intercept[IllegalArgumentException] {
      parseOption(Map(), argList)
    }
    assert(thrown.getMessage === "Unexpected or duplicate option flag: --strand")
  }

  @Test def testOptMinOverlapFraction() = {
    val argList = List("--minOverlapFraction", "0.8") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("minOverlapFraction") == 0.8)
  }

  @Test def testOptMinMapQ() = {
    val argList = List("--minMapQ", "13") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("minMapQ") == 13)
  }

  @Test def testOptStrandIdentical() = {
    val argList = List("--strand", "identical") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("strand") == Strand.Identical)
  }

  @Test def testOptStrandOpposite() = {
    val argList = List("--strand", "opposite") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("strand") == Strand.Opposite)
  }

  @Test def testOptStrandBoth() = {
    val argList = List("--strand", "both") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("strand") == Strand.Both)
  }

  @Test def testOptMakeIndex() = {
    val argList = List("--makeIndex") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("makeIndex") == true) // why can't we evaluate directly??
  }

  @Test def testOptLimitToRegion() = {
    val argList = List("--limitToRegion") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("limitToRegion") == true)
  }

  @Test def testOptSingleReadGroup() = {
    val argList = List("--readGroup", "g1") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("readGroup") == Vector("g1"))
  }

  @Test def testOptMultipleReadGroup() = {
    val argList = List("--readGroup", "g1,g2") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("readGroup") == Vector("g1", "g2"))
  }
}
