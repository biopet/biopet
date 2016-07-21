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

import htsjdk.samtools.{ QueryInterval, SamReaderFactory, ValidationStringency }
import htsjdk.samtools.fastq.{ BasicFastqWriter, FastqReader, FastqRecord }
import htsjdk.samtools.util.Interval
import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.collection.JavaConverters._
import scala.collection.mutable.{ Set => MSet }

object ExtractAlignedFastq extends ToolCommand {

  /** type alias for Fastq input (may or may not be paired) */
  type FastqInput = (FastqRecord, Option[FastqRecord])

  /** Get the FastqRecord ID */
  def fastqId(rec: FastqRecord) = rec.getReadHeader.split(" ")(0)

  /**
   * Function to create iterator over Interval given input interval string
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
  def makeIntervalFromString(inStrings: Iterable[String]): Iterator[Interval] = {

    // FIXME: can we combine these two patterns into one regex?
    // matches intervals with start and end coordinates
    val ptn1 = """([\w_-]+):([\d.,]+)-([\d.,]+)""".r
    // matches intervals with start coordinate only
    val ptn2 = """([\w_-]+):([\d.,]+)""".r
    // make ints from coordinate strings
    // NOTE: while it is possible for coordinates to exceed Int.MaxValue, we are limited
    // by the Interval constructor only accepting ints
    def intFromCoord(s: String): Int = s.replaceAll(",", "").replaceAll("\\.", "").toInt

    inStrings.map {
      case ptn1(chr, start, end) => new Interval(chr, intFromCoord(start), intFromCoord(end))
      case ptn2(chr, start) =>
        val startCoord = intFromCoord(start)
        new Interval(chr, startCoord, startCoord)
      case otherwise => throw new IllegalArgumentException("Invalid interval string: " + otherwise)
    }.toIterator
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
  def makeMembershipFunction(iv: Iterator[Interval],
                             inAln: File,
                             minMapQ: Int = 0,
                             commonSuffixLength: Int = 0): (FastqInput => Boolean) = {

    val inAlnReader = SamReaderFactory
      .make()
      .validationStringency(ValidationStringency.LENIENT)
      .open(inAln)
    require(inAlnReader.hasIndex)

    def getSequenceIndex(name: String): Int = inAlnReader.getFileHeader.getSequenceIndex(name) match {
      case x if x >= 0 =>
        x
      case otherwise =>
        throw new IllegalArgumentException("Chromosome " + name + " is not found in the alignment file")
    }

    val queries: Array[QueryInterval] = iv.toList
      // transform to QueryInterval
      .map(x => new QueryInterval(getSequenceIndex(x.getContig), x.getStart, x.getEnd))
      // sort Interval
      .sortBy(x => (x.referenceIndex, x.start, x.end))
      // cast to array
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

    (pair: FastqInput) => pair._2 match {
      case None => selected.contains(fastqId(pair._1))
      case Some(x) =>
        val rec1Id = fastqId(pair._1)
        require(commonSuffixLength < rec1Id.length)
        require(commonSuffixLength < fastqId(x).length)
        selected.contains(rec1Id.dropRight(commonSuffixLength))
    }
  }

  /**
   * Extracts reads from the given input Fastq file and writes to a new output Fastq file
   *
   * @param memFunc Predicate for extracting reads. If evaluates to true, the read is extracted.
   * @param inputFastq1 Input [[FastqReader]] object.
   * @param outputFastq1 Output [[BasicFastqWriter]] object.
   */
  def extractReads(memFunc: FastqInput => Boolean,
                   inputFastq1: FastqReader, outputFastq1: BasicFastqWriter): Unit =
    inputFastq1.iterator.asScala
      .filter(rec => memFunc((rec, None)))
      .foreach(rec => outputFastq1.write(rec))

  /**
   * Extracts reads from the given input Fastq pairs and writes to new output Fastq pair files
   *
   * @param memFunc Predicate for extracting reads. If evaluates to true, the read is extracted.
   * @param inputFastq1 Input [[FastqReader]] object for pair 1.
   * @param outputFastq1 Input [[FastqReader]] object for pair 2.
   * @param inputFastq2 Output [[BasicFastqWriter]] object for pair 1.
   * @param outputFastq2 Output [[BasicFastqWriter]] object for pair 2.
   */
  def extractReads(memFunc: FastqInput => Boolean,
                   inputFastq1: FastqReader, outputFastq1: BasicFastqWriter,
                   inputFastq2: FastqReader, outputFastq2: BasicFastqWriter): Unit =
    inputFastq1.iterator.asScala
      .zip(inputFastq2.iterator.asScala)
      .filter(rec => memFunc(rec._1, Some(rec._2)))
      .foreach(rec => {
        outputFastq1.write(rec._1)
        outputFastq2.write(rec._2)
      })

  /** Default arguments */
  case class Args(inputBam: File = new File(""),
                  intervals: List[String] = List.empty[String],
                  inputFastq1: File = new File(""),
                  inputFastq2: Option[File] = None,
                  outputFastq1: File = new File(""),
                  outputFastq2: Option[File] = None,
                  minMapQ: Int = 0,
                  commonSuffixLength: Int = 0) extends AbstractArgs

  /** Command line argument parser */
  class OptParser extends AbstractOptParser {

    head(
      s"""
        |$commandName - Select aligned FASTQ records
      """.stripMargin)

    opt[File]('I', "input_file") required () valueName "<bam>" action { (x, c) =>
      c.copy(inputBam = x)
    } validate {
      x => if (x.exists) success else failure("Input BAM file not found")
    } text "Input BAM file"

    opt[String]('r', "interval") required () unbounded () valueName "<interval>" action { (x, c) =>
      // yes, we are appending and yes it's O(n) ~ preserving order is more important than speed here
      c.copy(intervals = c.intervals :+ x)
    } text "Interval strings (e.g. chr1:1-100)"

    opt[File]('i', "in1") required () valueName "<fastq>" action { (x, c) =>
      c.copy(inputFastq1 = x)
    } validate {
      x => if (x.exists) success else failure("Input FASTQ file 1 not found")
    } text "Input FASTQ file 1"

    opt[File]('j', "in2") optional () valueName "<fastq>" action { (x, c) =>
      c.copy(inputFastq2 = Option(x))
    } validate {
      x => if (x.exists) success else failure("Input FASTQ file 2 not found")
    } text "Input FASTQ file 2 (default: none)"

    opt[File]('o', "out1") required () valueName "<fastq>" action { (x, c) =>
      c.copy(outputFastq1 = x)
    } text "Output FASTQ file 1"

    opt[File]('p', "out2") optional () valueName "<fastq>" action { (x, c) =>
      c.copy(outputFastq2 = Option(x))
    } text "Output FASTQ file 2 (default: none)"

    opt[Int]('Q', "min_mapq") optional () action { (x, c) =>
      c.copy(minMapQ = x)
    } text "Minimum MAPQ of reads in target region to remove (default: 0)"

    opt[Int]('s', "read_suffix_length") optional () action { (x, c) =>
      c.copy(commonSuffixLength = x)
    } text
      """Length of suffix mark from each read pair (default: 0). This is used for distinguishing read pairs with
         different suffices. For example, if your FASTQ records end with `/1` for the first pair and `/2` for the
         second pair, the value of `read_suffix_length` should be 2."
      """.stripMargin

    note(
      """
        |This tool creates FASTQ file(s) containing reads mapped to the given alignment intervals.
      """.stripMargin)

    checkConfig { c =>
      if (c.inputFastq2.isDefined && c.outputFastq2.isEmpty)
        failure("Missing output FASTQ file 2")
      else if (c.inputFastq2.isEmpty && c.outputFastq2.isDefined)
        failure("Missing input FASTQ file 2")
      else
        success
    }
  }

  /** Parses the command line argument */
  def parseArgs(args: Array[String]): Args =
    new OptParser()
      .parse(args, Args())
      .getOrElse(throw new IllegalArgumentException)

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    val memFunc = makeMembershipFunction(
      iv = makeIntervalFromString(commandArgs.intervals),
      inAln = commandArgs.inputBam,
      minMapQ = commandArgs.minMapQ,
      commonSuffixLength = commandArgs.commonSuffixLength)

    logger.info("Writing to output file(s) ...")
    (commandArgs.inputFastq2, commandArgs.outputFastq2) match {

      case (None, None) =>
        val in = new FastqReader(commandArgs.inputFastq1)
        val out = new BasicFastqWriter(commandArgs.outputFastq1)
        extractReads(memFunc, in, out)
        in.close()
        out.close()

      case (Some(i2), Some(o2)) =>
        val in1 = new FastqReader(commandArgs.inputFastq1)
        val in2 = new FastqReader(i2)
        val out1 = new BasicFastqWriter(commandArgs.outputFastq1)
        val out2 = new BasicFastqWriter(o2)
        extractReads(memFunc, in1, out1, in2, out2)
        in1.close()
        in2.close()
        out1.close()
        out2.close()

      case _ => ; // handled by the command line config check above
    }
  }
}
