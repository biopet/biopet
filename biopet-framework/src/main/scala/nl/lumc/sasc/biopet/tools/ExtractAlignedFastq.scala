/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.tribble.Feature
import htsjdk.tribble.BasicFeature

import nl.lumc.sasc.biopet.core.ToolCommand

object ExtractAlignedFastq extends ToolCommand {

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

  case class Args (inputBam: File = null,
                   intervals: List[String] = List.empty[String],
                   inputFastq1: File = null,
                   inputFastq2: File = null,
                   outputFastq1: File = null,
                   outputFastq2: File = null,
                   maxReadPerInterval: Int = Int.MaxValue,
                   minMapQ: Int = 0) extends AbstractArgs

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

    opt[File]("f1") required() valueName "<fastq>" action { (x, c) =>
      c.copy(inputFastq1 = x) } validate {
      x => if (x.exists) success else failure("Input FASTQ file 1 not found")
    } text "Input FASTQ file 1"

    opt[File]("f2") optional() valueName "<fastq>" action { (x, c) =>
      c.copy(inputFastq1 = x) } validate {
      x => if (x.exists) success else failure("Input FASTQ file 2 not found")
    } text "Input FASTQ file 2 (default: none)"

    opt[File]("o1") required() valueName "<fastq>" action { (x, c) =>
      c.copy(outputFastq1 = x) } text "Output FASTQ file 1"

    opt[File]("o2") optional() valueName "<fastq>" action { (x, c) =>
      c.copy(outputFastq1 = x) } text "Output FASTQ file 2 (default: none)"

    opt[Int]('N', "max_read_per_interval") optional() valueName "<num>" action { (x, c) =>
      c.copy(maxReadPerInterval = x)
    } text "Maximum number of reads to capture per given interval (default: " + Int.MaxValue.toString + ")"

    opt[Int]('Q', "min_mapq") optional() action { (x, c) =>
      c.copy(minMapQ = x) } text "Minimum MAPQ of reads in target region to remove (default: 0)"

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
  }
}
