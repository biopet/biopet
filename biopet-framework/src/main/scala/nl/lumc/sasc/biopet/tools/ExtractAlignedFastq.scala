/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File

import scala.collection.mutable.{ Set => MSet }
import scala.collection.JavaConverters._

import htsjdk.samtools.SAMFileReader
import htsjdk.samtools.QueryInterval
import htsjdk.samtools.fastq.{ BasicFastqWriter, FastqReader, FastqRecord }
import htsjdk.tribble.Feature
import htsjdk.tribble.BasicFeature

import nl.lumc.sasc.biopet.core.ToolCommand

object ExtractAlignedFastq extends ToolCommand {

  type FastqPair = (FastqRecord, FastqRecord)
  /**
   * Function to create iterator over features given input interval string
   *
   * Valid interval strings are either of these:
   *    - chr5:10000-11000
   *    - chr5:10,000-11,000
   *    - chr5:10.000-11.000
   *    - chr5:10000-11,000
   * In all cases above, the region span base #10,000 to base number #11,000 in chromosome 5
   * (first base is numbered 1)
   *
   * An interval string with a single base is also allowed:
   *    - chr5:10000
   *    - chr5:10,000
   *    - chr5:10.000
   *
   * @param inStrings iterable yielding input interval string
   */
  def makeFeatureFromString(inStrings: Iterable[String]): Iterator[Feature] = {

    // FIXME: can we combine these two patterns into one regex?
    // matches intervals with start and end coordinates
    val ptn1 = """([\w_-]+):([\d.,]+)-([\d.,]+)""".r
    // matches intervals with start coordinate only
    val ptn2 = """([\w_-]+):([\d.,]+)""".r
    // make ints from coordinate strings
    // NOTE: while it is possible for coordinates to exceed Int.MaxValue, we are limited
    // by the BasicFeature constructor only accepting ints
    def intFromCoord(s: String): Int = s.replaceAll(",", "").replaceAll("\\.", "").toInt

    inStrings.map(x => x match {
        case ptn1(chr, start, end)  => new BasicFeature(chr, intFromCoord(start), intFromCoord(end))
        case ptn2(chr, start)       => val startCoord = intFromCoord(start)
                                       new BasicFeature(chr, startCoord, startCoord)
        case _                      => throw new IllegalArgumentException("Invalid interval string: " + x)
      })
      .toIterator
  }

  /**
   * Function to create object that checks whether a given FASTQ record is mapped
   * to the given interval or not
   *
   * @param iv iterable yielding features to check
   * @param inAln input SAM/BAM file
   * @param minMapQ minimum mapping quality of read to include
   * @param commonSuffixLength length of suffix common to all read pairs
   * @return
   */
  def makeMembershipFunction(iv: Iterator[Feature],
                             inAln: File,
                             minMapQ: Int = 0,
                             commonSuffixLength: Int = 0
                            ): (FastqPair => Boolean) = {

    /** function to make interval queries for BAM files */
    def makeQueryInterval(aln: SAMFileReader, feat: Feature): QueryInterval =
      if (aln.getFileHeader.getSequenceIndex(feat.getChr) > -1)
        aln.makeQueryInterval(feat.getChr, feat.getStart, feat.getEnd)
      else if (feat.getChr.startsWith("chr")
        && aln.getFileHeader.getSequenceIndex(feat.getChr.substring(3)) > -1)
        aln.makeQueryInterval(feat.getChr.substring(3), feat.getStart, feat.getEnd)
      else if (!feat.getChr.startsWith("chr")
        && aln.getFileHeader.getSequenceIndex("chr" + feat.getChr) > -1)
        aln.makeQueryInterval("chr" + feat.getChr, feat.getStart, feat.getEnd)
      else
        throw new IllegalStateException("Unexpected feature: " + feat.toString)

    val inAlnReader = new SAMFileReader(inAln)
    require(inAlnReader.hasIndex)

    val queries: Array[QueryInterval] = iv.toList
      // sort features
      .sortBy(x => (x.getChr, x.getStart, x.getEnd))
      // turn them into QueryInterval objects
      .map(x => makeQueryInterval(inAlnReader, x))
      // return as an array
      .toArray

    lazy val selected: MSet[String] = inAlnReader
      // query BAM file for overlapping reads
      .queryOverlapping(queries)
      // for Scala compatibility
      .asScala
      // filter based on mapping quality
      .filter(x => x.getMappingQuality >= minMapQ)
      // iteratively add read name to the selected set
      .foldLeft(MSet.empty[String])(
        (acc, x) => {
          logger.debug("Adding " + x.getReadName + " to set ...")
          acc += x.getReadName
        }
       )

    (pair: FastqPair) => pair._2 match {
      case null       => selected.contains(pair._1.getReadHeader)
      case otherwise  =>
        require(commonSuffixLength < pair._1.getReadHeader.length)
        require(commonSuffixLength < pair._2.getReadHeader.length)
        selected.contains(pair._1.getReadHeader.dropRight(commonSuffixLength))
    }
  }

  def selectFastqReads(memFunc: FastqPair => Boolean,
                       inputFastq1: File,
                       outputFastq1: File,
                       inputFastq2: File = null,
                       outputFastq2: File = null): Unit = {

    val i1 = new FastqReader(inputFastq1).iterator.asScala
    val o1 = new BasicFastqWriter(outputFastq1)
    val i2 = inputFastq2 match {
      case null       => Iterator.continually(null)
      case otherwise  => new FastqReader(otherwise).iterator.asScala
    }
    val o2 = (inputFastq2, outputFastq2) match {
      case (null, null) => null
      case (_, null)    => throw new IllegalArgumentException("Missing output FASTQ 2")
      case (null, _)    => throw new IllegalArgumentException("Output FASTQ 2 supplied but there is no input FASTQ 2")
      case (x, y)       => new BasicFastqWriter(outputFastq2)
    }

    logger.info("Writing output file(s) ...")
    // zip, filter based on function, and write to output file(s)
    i1.zip(i2)
      .filter((rec) => memFunc(rec._1, rec._2))
      .foreach {
        case (rec1, null) =>
          o1.write(rec1)
        case (rec1, rec2) =>
          o1.write(rec1)
          o2.write(rec2)
    }

  }

  case class Args (inputBam: File = null,
                   intervals: List[String] = List.empty[String],
                   inputFastq1: File = null,
                   inputFastq2: File = null,
                   outputFastq1: File = null,
                   outputFastq2: File = null,
                   minMapQ: Int = 0,
                   commonSuffixLength: Int = 0) extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(
      s"""
        |$commandName - Select aligned FASTQ records
      """.stripMargin)

    opt[File]('I', "input_file") required() valueName "<bam>" action { (x, c) =>
      c.copy(inputBam = x) } validate {
      x => if (x.exists) success else failure("Input BAM file not found")
    } text "Input BAM file"

    opt[String]('r', "interval") required() unbounded() valueName "<interval>" action { (x, c) =>
      // yes, we are appending and yes it's O(n) ~ preserving order is more important than speed here
      c.copy(intervals = c.intervals :+ x) } text "Interval strings"

    opt[File]('i', "in1") required() valueName "<fastq>" action { (x, c) =>
      c.copy(inputFastq1 = x) } validate {
      x => if (x.exists) success else failure("Input FASTQ file 1 not found")
    } text "Input FASTQ file 1"

    opt[File]('j', "in2") optional() valueName "<fastq>" action { (x, c) =>
      c.copy(inputFastq1 = x) } validate {
      x => if (x.exists) success else failure("Input FASTQ file 2 not found")
    } text "Input FASTQ file 2 (default: none)"

    opt[File]('o', "out1") required() valueName "<fastq>" action { (x, c) =>
      c.copy(outputFastq1 = x) } text "Output FASTQ file 1"

    opt[File]('p', "out2") optional() valueName "<fastq>" action { (x, c) =>
      c.copy(outputFastq1 = x) } text "Output FASTQ file 2 (default: none)"

    opt[Int]('Q', "min_mapq") optional() action { (x, c) =>
      c.copy(minMapQ = x) } text "Minimum MAPQ of reads in target region to remove (default: 0)"

    opt[Int]('s', "read_suffix_length") optional() action { (x, c) =>
      c.copy(commonSuffixLength = x) } text "Length of common suffix from each read pair (default: 0)"

    note(
      """
        |This tool creates FASTQ file(s) containing reads mapped to the given alignment intervals.
      """.stripMargin)

    checkConfig { c=>
      if (c.inputFastq2 != null && c.outputFastq2 == null)
        failure("Missing output FASTQ file 2")
      else if (c.inputFastq2 == null && c.outputFastq2 != null)
        failure("Missing input FASTQ file 2")
      else
        success
    }
  }

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = new OptParser()
      .parse(args, Args())
      .getOrElse(sys.exit(1))

    val memFunc = makeMembershipFunction(
      iv = makeFeatureFromString(commandArgs.intervals),
      inAln = commandArgs.inputBam,
      minMapQ = commandArgs.minMapQ,
      commonSuffixLength = commandArgs.commonSuffixLength)

    selectFastqReads(memFunc,
      inputFastq1 = commandArgs.inputFastq1,
      inputFastq2 = commandArgs.inputFastq2,
      outputFastq1 = commandArgs.outputFastq1,
      outputFastq2 = commandArgs.outputFastq2)
  }
}
